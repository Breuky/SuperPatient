# -*- coding: utf-8 -*-
"""
Agent 对照实验：同一批问题分别跑 simple / agent，比较事实准确率、工具调用、耗时、token。

    python eval/run_agent_eval.py --record 1
"""
import argparse
import json
import time
import urllib.error
import urllib.request

QUESTIONS = [
    ("这次有哪些指标超出参考区间？", [["空腹血糖"], ["总胆固醇"]]),
    ("我的血糖最近怎么样？", [["6.4"]]),
    ("总胆固醇比上次高还是低？", [["5.6"], ["5.1"]]),
    ("这次血红蛋白正常吗？", [["142"]]),
    ("看看甘油三酯", [["未", "没有", "无"]]),
    ("这次检查是什么时间？", [["2026-03-28", "3月28", "03-28"]]),
    ("白细胞计数是多少？", [["6.8"]]),
    ("血小板计数是多少？", [["185"]]),
    ("相比上次，哪些指标变差了？", [["血糖"], ["胆固醇"]]),
    ("这次一共几项指标？", [["6"]]),
]


def call(base, method, path, body=None, token=None, timeout=120):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["token"] = token
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(base + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8"))


def flatten(insight):
    parts = [insight.get("summary") or ""]
    for h in insight.get("highlights") or []:
        parts.append((h.get("indicator") or "") + " " + (h.get("text") or ""))
    return "\n".join(parts)


def matched(text, groups):
    return all(any(k in text for k in group) for group in groups)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://localhost:8081")
    ap.add_argument("--user", default="demo")
    ap.add_argument("--password", default="demo1234")
    ap.add_argument("--record", type=int, default=1)
    args = ap.parse_args()

    token = call(args.base, "POST", "/api/auth/login",
                 {"username": args.user, "password": args.password})["data"]["token"]

    rows = []
    for mode in ("simple", "agent"):
        for question, groups in QUESTIONS:
            started = time.time()
            r = call(args.base, "POST", "/api/insights/report",
                     {"recordId": args.record, "mode": mode, "question": question}, token)
            elapsed = time.time() - started
            data = r.get("data") or {}
            text = flatten(data)
            ok = matched(text, groups) and not data.get("degraded")
            rows.append({
                "mode": mode, "question": question, "ok": ok,
                "elapsed": round(elapsed, 1),
                "turns": data.get("turns"),
                "tools": len(data.get("trace") or []),
                "tokens": (data.get("usage") or {}).get("total_tokens") or 0,
                "degraded": data.get("degraded"),
                "summary": data.get("summary"),
            })
            print("%-6s %-24s %s %5.1fs 轮%-2s 工具%-2s %5stok  %s" % (
                mode, question[:22], "PASS" if ok else "MISS",
                elapsed, data.get("turns"), len(data.get("trace") or []),
                (data.get("usage") or {}).get("total_tokens") or 0,
                (data.get("summary") or "")[:40]))
        print("-" * 100)

    print()
    print("| 模式 | 事实命中 | 平均耗时 | 平均工具调用 | 平均 token | 降级 |")
    print("|---|---|---|---|---|---|")
    for mode in ("simple", "agent"):
        subset = [r for r in rows if r["mode"] == mode]
        hit = sum(1 for r in subset if r["ok"])
        n = len(subset)
        print("| %s | %d/%d (%.0f%%) | %.1fs | %.1f | %d | %d |" % (
            mode, hit, n, hit * 100.0 / n,
            sum(r["elapsed"] for r in subset) / n,
            sum(r["tools"] for r in subset) / n,
            sum(r["tokens"] for r in subset) / n,
            sum(1 for r in subset if r["degraded"])))

    with open("eval/agent_eval.json", "w", encoding="utf-8") as f:
        json.dump(rows, f, ensure_ascii=False, indent=1)
    print()
    print("明细已写入 eval/agent_eval.json")


if __name__ == "__main__":
    main()
