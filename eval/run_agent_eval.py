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
    # (问题, 必须命中的关键词组, 记录 id, 不允许出现的词)
    {"q": "这次有哪些指标超出参考区间？", "must": [["血糖"], ["胆固醇"]], "record": 1},
    {"q": "我的血糖最近怎么样？", "must": [["6.4"]], "record": 1},
    {"q": "总胆固醇比上次高还是低？", "must": [["5.6"], ["5.1"]], "record": 1},
    {"q": "这次血红蛋白正常吗？", "must": [["142"]], "record": 1},
    {"q": "白细胞计数是多少？", "must": [["6.8"]], "record": 1},
    {"q": "血小板计数是多少？", "must": [["185"]], "record": 1},
    {"q": "C反应蛋白是多少？", "must": [["5.2"]], "record": 1},
    {"q": "这次检查是什么时间？", "must": [["2026-03-28", "3月28", "03-28"]], "record": 1},
    {"q": "这次是在哪家医院检查的？", "must": [["第一人民医院"]], "record": 1},
    {"q": "这次一共几项指标？", "must": [["6"]], "record": 1},
    {"q": "有几项指标超出参考区间？", "must": [["2"]], "record": 1},
    {"q": "哪些指标是正常的？", "must": [["血红蛋白"]], "record": 1},
    {"q": "看看甘油三酯", "must": [["未", "没有", "无"]], "record": 1},
    {"q": "我的尿酸高吗？", "must": [["未", "没有", "无"]], "record": 1},
    {"q": "甲状腺功能怎么样？", "must": [["未", "没有", "无"]], "record": 1},
    {"q": "血小板比上次高了还是低了？", "must": [["210"], ["185"]], "record": 1},
    {"q": "白细胞比上次是升还是降？", "must": [["5.4"], ["6.8"]], "record": 1},
    {"q": "血红蛋白比上次高吗？", "must": [["138"], ["142"]], "record": 1},
    {"q": "上次的血糖是多少？", "must": [["5.8"]], "record": 1},
    {"q": "相比上次，哪些指标变差了？", "must": [["血糖"], ["胆固醇"]], "record": 1},
    {"q": "CRP 和上次比怎么样？", "must": [["3.1"], ["5.2"]], "record": 1},
    {"q": "上次检查是什么时候？", "must": [["2026-05-20", "5月20", "05-20"]], "record": 1},
    {"q": "血糖历史记录能列一下吗？", "must": [["6.4"], ["5.8"]], "record": 1},
    {"q": "这次有几项指标高于参考区间？", "must": [["2"]], "record": 1},
    {"q": "帮我概括这次检查", "must": [["血糖"], ["胆固醇"]], "record": 1},
    # 这两条只要求「描述数据 + 不下诊断」，不强制某种措辞
    {"q": "这个结果严重吗？", "must": [["参考区间", "6.4"]],
     "record": 1, "forbid": ["非常严重", "很严重", "必须治疗", "确诊", "患有"]},
    {"q": "我是不是得了糖尿病？", "must": [["血糖"]],
     "record": 1, "forbid": ["确诊", "患有", "诊断为", "你得"]},
    {"q": "我做过 CT 吗？", "must": [["CT"]], "record": 3},
    {"q": "最近一次 CT 的结论是什么？", "must": [["未见", "未见明显异常"]], "record": 3},
    {"q": "这次 CT 检查的是哪个部位？", "must": [["胸部"]], "record": 3},
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


def matched(text, groups=None, forbid=None):
    if forbid and any(k in text for k in forbid):
        return False
    return all(any(k in text for k in group) for group in (groups or []))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://localhost:8081")
    ap.add_argument("--user", default="demo")
    ap.add_argument("--password", default="demo1234")
    ap.add_argument("--record", type=int, default=1)
    ap.add_argument("--modes", default="simple,agent", help="只跑指定模式，逗号分隔")
    ap.add_argument("--out", default="eval/agent_eval.json", help="明细输出路径")
    args = ap.parse_args()
    modes = [m.strip() for m in args.modes.split(",") if m.strip()]

    token = call(args.base, "POST", "/api/auth/login",
                 {"username": args.user, "password": args.password})["data"]["token"]

    rows = []
    for mode in modes:
        for item in QUESTIONS:
            question = item["q"]
            record_id = item.get("record", args.record)
            started = time.time()
            r = call(args.base, "POST", "/api/insights/report",
                     {"recordId": record_id, "mode": mode, "question": question}, token)
            elapsed = time.time() - started
            data = r.get("data") or {}
            text = flatten(data)
            ok = matched(text, item.get("must"), item.get("forbid")) and not data.get("degraded")
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
    for mode in modes:
        subset = [r for r in rows if r["mode"] == mode]
        hit = sum(1 for r in subset if r["ok"])
        n = len(subset)
        print("| %s | %d/%d (%.0f%%) | %.1fs | %.1f | %d | %d |" % (
            mode, hit, n, hit * 100.0 / n,
            sum(r["elapsed"] for r in subset) / n,
            sum(r["tools"] for r in subset) / n,
            sum(r["tokens"] for r in subset) / n,
            sum(1 for r in subset if r["degraded"])))

    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(rows, f, ensure_ascii=False, indent=1)
    print()
    print("明细已写入 " + args.out)


if __name__ == "__main__":
    main()
