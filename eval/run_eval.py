# -*- coding: utf-8 -*-
"""
抽取评测：把标注（labels/*.csv）与抽取结果（raw/*.json）逐行比对。

指标:
  召回率      = 标注里有、且被抽到的行 / 标注总行数
  数值准确率  = 抽到且数值一致的行 / 抽到的行
  单位准确率  = 抽到且单位一致的行 / 抽到的行
  归一化率    = 抽到且成功归到指标库的行 / 抽到的行
  误报        = 抽到了但标注里没有的行

用法:
    python eval/run_eval.py                 # 用已有 raw2 结果（不花钱）
    python eval/run_eval.py --raw raw3      # 指定一次结果目录
"""
import argparse
import csv
import io
import json
import os
import re

COMPARATOR = re.compile(r"^[<>≤≥＜＞]=?")


def norm_name(s):
    if not s:
        return ""
    s = s.replace("\u3000", "")
    s = re.sub(r"\s+", "", s)
    s = (s.replace("（", "(").replace("）", ")")
          .replace("％", "%").replace("：", ":")).upper()
    s = re.sub(r"\([^)]*\)", "", s)
    s = re.sub(r"\[[^]]*\]", "", s)
    s = re.sub(r"[-－—–_]", "", s)
    s = re.sub(r"^[*＊↑↓▲△☆#]+", "", s)
    s = re.sub(r"[*＊↑↓▲△☆.,，。;；]+$", "", s)
    return s


def norm_unit(u):
    if not u:
        return ""
    u = u.replace(" ", "").replace("μ", "u").replace("µ", "u")
    u = u.replace("×", "x").upper()
    u = re.sub(r"^X?10[\^\*]?(\d+)/L$", r"10^\1/L", u)
    if u in ("U/L", "IU/L"):
        return "U/L"
    return u


def parse_number(v):
    if v is None or v == "":
        return None
    s = str(v).strip()
    s = COMPARATOR.sub("", s)
    s = s.replace(",", "").replace("↑", "").replace("↓", "")
    try:
        return float(s)
    except ValueError:
        return None


def close_enough(a, b):
    if a is None or b is None:
        return False
    return abs(a - b) <= max(abs(b) * 0.005, 1e-6)


def load_labels(path):
    rows = []
    with io.open(path, encoding="utf-8-sig") as f:
        for r in csv.DictReader(f):
            if not r.get("name") or not (r.get("value") or "").strip():
                continue
            rows.append({
                "name": r["name"].strip(),
                "value": (r.get("value") or "").strip(),
                "unit": (r.get("unit") or "").strip(),
                "refMin": (r.get("refMin") or "").strip(),
                "refMax": (r.get("refMax") or "").strip(),
            })
    return rows


def load_raw(path):
    data = json.load(io.open(path, encoding="utf-8"))
    if data.get("code") != 1:
        return []
    d = data["data"]
    out = []
    for c in d.get("candidates", []):
        out.append({
            "rawName": c.get("rawName") or "",
            "indicatorId": c.get("indicatorId"),
            "value": c.get("valueDecimal") if c.get("valueDecimal") is not None else c.get("valueText"),
            "unit": c.get("unit") or c.get("rawUnit") or "",
        })
    for u in d.get("unmatched", []):
        out.append({
            "rawName": u.get("rawName") or "",
            "indicatorId": None,
            "value": u.get("rawValue"),
            "unit": u.get("rawUnit") or "",
        })
    return out


def evaluate(labels, candidates):
    by_name = {}
    for c in candidates:
        by_name.setdefault(norm_name(c["rawName"]), []).append(c)

    stats = {"labels": len(labels), "matched": 0, "value_ok": 0,
             "unit_ok": 0, "normalized": 0, "fp": 0}
    missing, wrong_value, wrong_unit = [], [], []
    used = set()

    for lb in labels:
        key = norm_name(lb["name"])
        cand = None
        for c in by_name.get(key, []):
            if id(c) not in used:
                cand = c
                break
        if cand is None:
            missing.append(lb["name"])
            continue
        used.add(id(cand))
        stats["matched"] += 1

        lv, cv = parse_number(lb["value"]), parse_number(cand["value"])
        if lv is not None and cv is not None:
            if close_enough(cv, lv):
                stats["value_ok"] += 1
            else:
                wrong_value.append("%s: 标注 %s / 抽出 %s" % (lb["name"], lb["value"], cand["value"]))
        elif str(lb["value"]) == str(cand["value"]):
            stats["value_ok"] += 1

        if norm_unit(lb["unit"]) == norm_unit(cand["unit"]):
            stats["unit_ok"] += 1
        else:
            wrong_unit.append("%s: 标注 %s / 抽出 %s" % (lb["name"], lb["unit"], cand["unit"]))

        if cand["indicatorId"] is not None:
            stats["normalized"] += 1

    stats["fp"] = sum(1 for c in candidates if id(c) not in used)
    return stats, missing, wrong_value, wrong_unit


def pct(a, b):
    return 0.0 if b == 0 else a * 100.0 / b


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raw", default="raw2")
    ap.add_argument("--labels", default="labels")
    args = ap.parse_args()

    root = os.path.dirname(os.path.abspath(__file__))
    raw_dir = os.path.join(root, args.raw)
    label_dir = os.path.join(root, args.labels)

    total = {"labels": 0, "matched": 0, "value_ok": 0, "unit_ok": 0, "normalized": 0, "fp": 0}
    lines = ["| 样本 | 标注行 | 抽到 | 召回率 | 数值准确 | 单位准确 | 归一化 | 误报 |",
             "|---|---|---|---|---|---|---|---|"]

    for fn in sorted(os.listdir(label_dir)):
        if not fn.endswith(".csv"):
            continue
        name = fn[:-4]
        raw_path = os.path.join(raw_dir, name + ".json")
        if not os.path.exists(raw_path):
            print("缺少抽取结果:", raw_path)
            continue
        labels = load_labels(os.path.join(label_dir, fn))
        candidates = load_raw(raw_path)
        s, missing, wv, wu = evaluate(labels, candidates)
        for k in total:
            total[k] += s[k]
        lines.append("| %s | %d | %d | %.1f%% | %.1f%% | %.1f%% | %.1f%% | %d |" % (
            name, s["labels"], s["matched"],
            pct(s["matched"], s["labels"]),
            pct(s["value_ok"], s["matched"]),
            pct(s["unit_ok"], s["matched"]),
            pct(s["normalized"], s["matched"]),
            s["fp"]))
        if missing:
            print("[%s] 未抽到: %s" % (name, "、".join(missing)))
        if wv:
            print("[%s] 数值不符: %s" % (name, "; ".join(wv)))
        if wu:
            print("[%s] 单位不符: %s" % (name, "; ".join(wu)))

    lines.append("| **合计** | %d | %d | **%.1f%%** | **%.1f%%** | **%.1f%%** | **%.1f%%** | %d |" % (
        total["labels"], total["matched"],
        pct(total["matched"], total["labels"]),
        pct(total["value_ok"], total["matched"]),
        pct(total["unit_ok"], total["matched"]),
        pct(total["normalized"], total["matched"]),
        total["fp"]))

    print()
    print("\n".join(lines))
    io.open(os.path.join(root, "report.md"), "w", encoding="utf-8").write(
        "# 抽取评测报告\n\n" + "\n".join(lines) + "\n")
    print("\n已写入 eval/report.md")


if __name__ == "__main__":
    main()
