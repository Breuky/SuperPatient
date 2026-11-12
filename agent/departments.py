# -*- coding: utf-8 -*-
"""
指标异常 → 建议就诊科室（纯规则，不调模型）

设计原则
1. 只有「异常指标」参与判定：结构化的、已验证的数据比自由文本可靠。
2. 只输出候选科室 + 依据，不输出「你得了什么病」。
3. 模型可以在候选内排序/解释（后续 L3 工具），但不能新增科室——白名单校验在工具层做。
"""

from __future__ import annotations

# 科室名用全称，前端展示与医院科室字段对齐
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
GENERAL = "全科医学科"

# (关键词, 科室, 权重, 说明)
# 权重含义：「这个指标异常对该科室的指向性有多强」，不是严重程度。
RULES = (
    # —— 代谢 / 内分泌
    ("糖化血红蛋白", ENDOCRINE, 3.0, "反映近 2-3 个月平均血糖"),
    ("空腹血糖", ENDOCRINE, 3.0, "空腹血糖异常提示糖代谢问题"),
    ("餐后血糖", ENDOCRINE, 2.5, "餐后血糖异常"),
    ("胰岛素", ENDOCRINE, 2.0, "胰岛素相关指标"),
    ("c肽", ENDOCRINE, 2.0, "胰岛功能指标"),
    ("促甲状腺", ENDOCRINE, 3.0, "甲状腺功能"),
    ("tsh", ENDOCRINE, 3.0, "甲状腺功能"),
    ("ft3", ENDOCRINE, 2.5, "甲状腺功能"),
    ("ft4", ENDOCRINE, 2.5, "甲状腺功能"),
    ("游离甲状腺", ENDOCRINE, 2.5, "甲状腺功能"),
    ("总胆固醇", ENDOCRINE, 2.0, "血脂异常"),
    ("甘油三酯", ENDOCRINE, 2.0, "血脂异常"),
    ("低密度脂蛋白", ENDOCRINE, 2.0, "血脂异常"),
    ("高密度脂蛋白", ENDOCRINE, 1.5, "血脂异常"),
    # —— 肾脏 / 泌尿
    ("肌酐", NEPHRO, 3.0, "肾功能核心指标"),
    ("尿素", NEPHRO, 2.0, "肾功能指标"),
    ("尿蛋白", NEPHRO, 2.5, "蛋白尿提示肾损伤"),
    ("微量白蛋白", NEPHRO, 2.5, "早期肾损伤指标"),
    ("胱抑素", NEPHRO, 2.0, "肾功能指标"),
    ("肾小球滤过", NEPHRO, 3.0, "eGFR 直接反映肾功能"),
    ("尿潜血", UROLOGY, 2.0, "血尿需排查泌尿系统"),
    ("尿红细胞", UROLOGY, 2.0, "血尿"),
    ("前列腺特异", UROLOGY, 3.0, "前列腺相关"),
    # —— 消化 / 肝胆
    ("谷丙", GASTRO, 3.0, "肝细胞损伤最敏感的指标之一"),
    ("alt", GASTRO, 3.0, "肝细胞损伤"),
    ("谷草", GASTRO, 2.5, "肝细胞/心肌损伤"),
    ("ast", GASTRO, 2.5, "肝细胞/心肌损伤"),
    ("谷氨酰转肽酶", HEPATOLOGY, 2.0, "胆道/酒精相关肝损伤"),
    ("ggt", HEPATOLOGY, 2.0, "胆道相关"),
    ("碱性磷酸酶", HEPATOLOGY, 2.0, "胆道/骨代谢"),
    ("总胆红素", HEPATOLOGY, 2.5, "黄疸相关"),
    ("直接胆红素", HEPATOLOGY, 2.5, "胆汁淤积相关"),
    ("间接胆红素", HEPATOLOGY, 2.0, "溶血/肝摄取相关"),
    ("白蛋白", HEPATOLOGY, 1.5, "肝脏合成功能（也受营养影响）"),
    ("总蛋白", HEPATOLOGY, 1.0, "蛋白代谢"),
    # —— 血液
    ("白细胞", HEMATO, 1.5, "血常规"),
    ("红细胞", HEMATO, 1.5, "血常规"),
    ("血红蛋白", HEMATO, 1.5, "贫血相关"),
    ("血小板", HEMATO, 1.5, "血常规"),
    ("红细胞压积", HEMATO, 1.0, "血常规"),
    ("中性粒细胞", HEMATO, 1.0, "血常规"),
    ("淋巴细胞", HEMATO, 1.0, "血常规"),
    ("平均红细胞体积", HEMATO, 1.0, "贫血分型"),
    # —— 心血管
    ("肌钙蛋白", CARDIO, 3.0, "心肌损伤标志物"),
    ("ck-mb", CARDIO, 2.5, "心肌损伤"),
    ("肌酸激酶同工酶", CARDIO, 2.5, "心肌损伤"),
    ("bnp", CARDIO, 2.5, "心功能"),
    ("利钠肽", CARDIO, 2.5, "心功能"),
    # —— 感染 / 炎症
    ("c反应蛋白", INFECT, 1.5, "炎症指标"),
    ("crp", INFECT, 1.5, "炎症指标"),
    ("降钙素原", INFECT, 2.0, "细菌感染指标"),
    ("血沉", RHEUM, 1.5, "炎症/风湿活动度"),
    # —— 风湿免疫
    ("尿酸", RHEUM, 2.0, "高尿酸血症（也可看内分泌）"),
    ("类风湿因子", RHEUM, 2.5, "风湿免疫"),
    ("抗ccp", RHEUM, 2.5, "类风湿相关"),
    ("抗核抗体", RHEUM, 2.5, "自身免疫"),
    # —— 影像（按部位）
    ("胸部ct", RESPIR, 2.5, "胸部影像"),
    ("肺ct", RESPIR, 2.5, "胸部影像"),
    ("头颅", NEURO, 2.5, "头颅影像"),
    ("脑ct", NEURO, 2.5, "头颅影像"),
    ("脑mri", NEURO, 2.5, "头颅影像"),
)

# 同一指标可能同时指向多个科室时，补一条次选（权重递减）
SECONDARY = {
    "尿酸": ((ENDOCRINE, 1.0),),
    "尿潜血": ((NEPHRO, 1.5),),
    "谷草": ((CARDIO, 1.0),),
    "白细胞": ((INFECT, 1.0),),
    "血沉": ((INFECT, 0.8),),
}


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
        for keyword, dept, weight, note in RULES:
            if keyword and keyword in norm:
                scores[dept] = scores.get(dept, 0.0) + weight
                reasons.setdefault(dept, [])
                if note not in reasons[dept]:
                    reasons[dept].append(note)
                hit = True
                for extra_dept, extra_weight in SECONDARY.get(keyword, ()):
                    scores[extra_dept] = scores.get(extra_dept, 0.0) + extra_weight
                    reasons.setdefault(extra_dept, [])
                    if note not in reasons[extra_dept]:
                        reasons[extra_dept].append(note)
                break
        if not hit:
            unmapped.append(name)

    if not scores:
        return {
            "departments": [{
                "name": GENERAL,
                "score": 0.0,
                "reasons": ["没有可映射的异常指标"],
            }],
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
