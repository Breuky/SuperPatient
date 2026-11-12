# -*- coding: utf-8 -*-
"""
指标异常 → 建议就诊科室（纯规则，不调模型）

设计原则
1. 只有「异常指标」参与判定：结构化的、已验证的数据比自由文本可靠。
2. 只输出候选科室 + 依据，不输出「你得了什么病」。
3. 模型可以在候选内排序/解释（L3 工具），但不能新增科室——白名单校验在工具层做。

规则不写在代码里，外置在 data/departments.csv：
  keyword,group,department,weight,secondary,note,source
  · group     检验医学的功能分组，用来解释「为什么指向这个科室」
  · weight    该指标对这个科室的指向性（不是严重程度）
  · secondary 次选科室，格式「科室:权重」，多个用分号
  · source    这条规则的来源，便于核对

改规则只改 CSV，不改代码。
"""

from __future__ import annotations

import csv
import os

ENDOCRINE = "内分泌科"
NEPHRO = "肾内科"
UROLOGY = "泌尿外科"
GASTRO = "消化内科"
HEPATOLOGY = "肝病科"
HEMATO = "血液科"
CARDIO = "心血管内科"
INFECT = "感染科"
RESPIR = "呼吸内科"
NEURO = "神经内科"
RHEUM = "风湿免疫科"
VASCULAR = "血管外科"
GENERAL = "全科医学科"

_CSV_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "departments.csv")


def _load_rules(path=_CSV_PATH):
    """读取规则表；文件缺失时直接失败，不静默降级成空规则。"""
    rules = []
    with open(path, encoding="utf-8-sig", newline="") as f:
        for row in csv.DictReader(f):
            keyword = (row.get("keyword") or "").strip().lower()
            department = (row.get("department") or "").strip()
            if not keyword or not department:
                continue
            try:
                weight = float(row.get("weight") or 0)
            except ValueError:
                weight = 0.0
            secondary = []
            for part in (row.get("secondary") or "").split(";"):
                part = part.strip()
                if ":" in part:
                    dept, raw_weight = part.split(":", 1)
                    try:
                        secondary.append((dept.strip(), float(raw_weight)))
                    except ValueError:
                        continue
            rules.append((keyword, department, weight,
                          (row.get("note") or "").strip(), tuple(secondary)))
    if not rules:
        raise RuntimeError("科室规则表为空: " + path)
    return tuple(rules)


RULES = _load_rules()


def _norm(name: str) -> str:
    return (name or "").strip().lower().replace(" ", "")


def suggest(abnormal_indicators):
    """
    abnormal_indicators: [{"name": "谷丙转氨酶", "direction": "high"}]
    返回 {"departments": [{name, score, reasons}], "unmapped": [name...]}
    """
    scores = {}
    reasons = {}
    unmapped = []

    for item in abnormal_indicators or ():
        name = item.get("name") if isinstance(item, dict) else str(item)
        norm = _norm(name)
        hit = False
        for keyword, dept, weight, note, secondary in RULES:
            if keyword not in norm:
                continue
            scores[dept] = scores.get(dept, 0.0) + weight
            reasons.setdefault(dept, [])
            if note and note not in reasons[dept]:
                reasons[dept].append(note)
            for extra_dept, extra_weight in secondary:
                scores[extra_dept] = scores.get(extra_dept, 0.0) + extra_weight
                reasons.setdefault(extra_dept, [])
                if note and note not in reasons[extra_dept]:
                    reasons[extra_dept].append(note)
            hit = True
            break
        if not hit:
            unmapped.append(name)

    if not scores:
        return {
            "departments": [{"name": GENERAL, "score": 0.0,
                             "reasons": ["没有可映射的异常指标"]}],
            "unmapped": unmapped,
        }

    ranked = sorted(scores.items(), key=lambda kv: (-kv[1], kv[0]))
    top = ranked[0][1]
    return {
        "departments": [{
            "name": dept,
            "score": round(score / top, 2),
            "reasons": reasons.get(dept, [])[:3],
        } for dept, score in ranked[:3]],
        "unmapped": unmapped,
    }
