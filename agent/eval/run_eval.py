# -*- coding: utf-8 -*-
"""
端到端评测：跑一遍病例集，统计三件事
  1) 工具命中率  —— 该调的工具有没有调
  2) 越界拦截率  —— 医学判断类是否被置 out_of_scope
  3) 引用正确率  —— 知识题的命中来源是否包含预期来源
另附：无据作答率、平均轮数、平均耗时

用法：
  python eval/run_eval.py            # 全量
  python eval/run_eval.py --limit 5  # 先跑前 5 条
"""

import argparse
import json
import os
import sys
import urllib.request
from collections import Counter

BASE = os.environ.get("SP_BASE", "http://localhost:8081")
CASES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "cases.json")


def post(path, payload, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["token"] = token
    req = urllib.request.Request(BASE + path, data=json.dumps(payload, ensure_ascii=False).encode(),
                                 headers=headers, method="POST")
    with urllib.request.urlopen(req, timeout=180) as resp:
        return json.loads(resp.read().decode("utf-8"))


def login():
    body = post("/api/auth/login", {"username": "demo", "password": "demo1234"})
    return body["data"]["token"]


def run_case(case, token):
    data = post("/api/insights/report",
                {"mode": "agent", "question": case["question"]}, token)["data"]
    trace = [t.get("tool") for t in (data.get("trace") or [])]
    hits = (data.get("knowledge") or {}).get("hits") or []
    return {
        "id": case["id"],
        "question": case["question"],
        "capability": data.get("capability"),
        "out_of_scope": bool(data.get("outOfScope")),
        "off_topic": bool(data.get("offTopic")),
        "tools": trace,
        "source_ids": [h.get("source_id") for h in hits],
        "turns": data.get("turns"),
        "elapsed_ms": sum((t.get("ms") or 0) for t in (data.get("trace") or [])) + (data.get("elapsedMs") or 0),
        "summary_len": len(data.get("summary") or ""),
        "no_tool": not trace,
    }


def judge(case, res):
    exp = case.get("expect") or {}
    checks = {}

    if "capability" in exp:
        checks["capability"] = (res["capability"] == exp["capability"])

    tools_any = exp.get("tools_any")
    if tools_any is not None:
        if not tools_any:
            checks["tool_hit"] = res["no_tool"]      # 期望「不调工具」
        else:
            checks["tool_hit"] = any(t in tools_any for t in res["tools"])

    if "source_ids" in exp:
        checks["citation"] = bool(set(exp["source_ids"]) & set(res["source_ids"]))

    if exp.get("out_of_scope"):
        checks["refuse"] = res["out_of_scope"]

    if exp.get("off_topic"):
        checks["off_topic"] = res["off_topic"]

    if exp.get("refuse_like"):
        checks["refuse_like"] = res["no_tool"] or res["capability"] == "L3"

    # 无据作答：没调任何工具却给了像样的回答（越界题除外）
    if not exp.get("off_topic"):
        checks["grounded"] = (not res["no_tool"]) or res["capability"] in ("L3", "L2")

    return checks


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--delay", type=float, default=7.0, help="每个用例之间的间隔秒数（避开 10 次/分钟限流）")
    ap.add_argument("--out", default="eval/report.md")
    args = ap.parse_args()

    cases = json.load(open(CASES, encoding="utf-8"))["cases"]
    if args.limit:
        cases = cases[:args.limit]

    token = login()
    rows, agg = [], Counter()
    import time
    for i, case in enumerate(cases):
        if i:
            time.sleep(args.delay)
        try:
            res = run_case(case, token)
        except Exception as e:                    # noqa: BLE001
            print("!! %s 调用失败: %s" % (case["id"], e))
            continue
        checks = judge(case, res)
        for k, v in checks.items():
            agg[k] += 1
            agg[k + "_pass"] += 1 if v else 0
        rows.append((case, res, checks))
        print("%-8s %-34s cap=%-3s tools=%-34s %s" % (
            case["id"], case["question"][:32], res["capability"],
            ",".join(res["tools"]) or "-",
            " ".join("%s=%s" % (k, "✓" if v else "✗") for k, v in checks.items())))

    total = len(rows)
    lines = ["# Agent 评测报告", ""]
    lines.append("- 病例数：%d" % total)
    lines.append("- 平均轮数：%.2f" % (sum(r[1]["turns"] or 0 for r in rows) / max(1, total)))
    lines.append("- 平均耗时：%.0f ms" % (sum(r[1]["elapsed_ms"] or 0 for r in rows) / max(1, total)))
    lines.append("")
    lines.append("| 指标 | 通过 / 总数 | 通过率 |")
    lines.append("|---|---|---|")
    names = {"capability": "能力分级正确", "tool_hit": "工具命中",
             "citation": "引用正确", "refuse": "越界拦截",
             "off_topic": "离题拦截", "refuse_like": "代办拦截", "grounded": "无据作答(应通过)"}
    for k, label in names.items():
        if agg[k]:
            lines.append("| %s | %d / %d | %.0f%% |" % (label, agg[k + "_pass"], agg[k],
                                                         100.0 * agg[k + "_pass"] / agg[k]))
    lines.append("")
    lines.append("| 用例 | 问题 | 级别 | 工具 | 摘要长度 |")
    lines.append("|---|---|---|---|---|")
    for case, res, _ in rows:
        lines.append("| %s | %s | %s | %s | %d |" % (
            case["id"], case["question"], res["capability"],
            ",".join(res["tools"]) or "-", res["summary_len"]))

    out = os.path.join(os.path.dirname(CASES), os.path.basename(args.out))
    open(out, "w", encoding="utf-8").write("\n".join(lines))
    print()
    print("\n".join(lines[:12]))
    print("\n报告已写入:", out)


if __name__ == "__main__":
    main()