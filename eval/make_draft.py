# -*- coding: utf-8 -*-
"""
从一次抽取结果生成标注草稿（CSV）。
草稿只是把模型输出整理成可编辑表格，**必须对着原图逐行核对**：
模型漏掉的行不会出现在草稿里，需要手工补。

用法:
    python eval/make_draft.py                 # 默认读 eval/raw2
    python eval/make_draft.py --raw eval/raw  # 指定来源
"""
import argparse
import csv
import io
import json
import os

COLUMNS = ["name", "value", "unit", "refMin", "refMax"]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raw", default="raw2")
    ap.add_argument("--out", default="labels")
    args = ap.parse_args()

    root = os.path.dirname(os.path.abspath(__file__))
    raw_dir = os.path.join(root, args.raw)
    out_dir = os.path.join(root, args.out)
    os.makedirs(out_dir, exist_ok=True)

    for fn in sorted(os.listdir(raw_dir)):
        if not fn.endswith(".json"):
            continue
        data = json.load(io.open(os.path.join(raw_dir, fn), encoding="utf-8"))
        if data.get("code") != 1:
            print("跳过（抽取失败）:", fn)
            continue
        d = data["data"]
        rows = []
        for c in d.get("candidates", []) + d.get("unmatched", []):
            if c.get("valueDecimal") is not None:
                value = str(c["valueDecimal"]).rstrip("0").rstrip(".")
            elif c.get("valueText"):
                value = c["valueText"]
            elif c.get("rawValue"):
                value = c["rawValue"]
            else:
                # 分组标题没有值，跳过
                continue
            unit = c.get("unit") or c.get("rawUnit") or ""
            rows.append([
                c.get("rawName") or "",
                value,
                unit,
                "" if c.get("refMin") is None else str(c["refMin"]),
                "" if c.get("refMax") is None else str(c["refMax"]),
            ])

        out_path = os.path.join(out_dir, fn.replace(".json", ".csv"))
        with io.open(out_path, "w", encoding="utf-8-sig", newline="") as f:
            w = csv.writer(f)
            w.writerow(COLUMNS)
            w.writerows(rows)
        print("生成", os.path.relpath(out_path, root), "共", len(rows), "行")


if __name__ == "__main__":
    main()
