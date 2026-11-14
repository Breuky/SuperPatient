# -*- coding: utf-8 -*-
"""
并发压测：验证「限流是否生效 + 高并发下是否还有可用响应」
关心三件事：成功率、429 比例、延迟分位数；失败时是否降级而不是报错

用法：python eval/load_test.py --concurrency 8 --total 16
"""

import argparse
import json
import statistics
import sys
import threading
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor

BASE = "http://localhost:8081"


def login():
    req = urllib.request.Request(BASE + "/api/auth/login",
                                 data=json.dumps({"username": "demo", "password": "demo1234"}).encode(),
                                 headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode())["data"]["token"]


def one(token, question):
    started = time.time()
    try:
        req = urllib.request.Request(BASE + "/api/insights/report",
                                     data=json.dumps({"mode": "agent", "question": question},
                                                     ensure_ascii=False).encode(),
                                     headers={"Content-Type": "application/json", "token": token},
                                     method="POST")
        with urllib.request.urlopen(req, timeout=120) as resp:
            body = json.loads(resp.read().decode())
        elapsed = (time.time() - started) * 1000
        data = body.get("data") or {}
        return {"status": "ok", "ms": elapsed, "degraded": bool(data.get("degraded")),
                "turns": data.get("turns")}
    except urllib.error.HTTPError as e:
        return {"status": "http_%d" % e.code, "ms": (time.time() - started) * 1000}
    except Exception as e:                                   # noqa: BLE001
        return {"status": "error:" + type(e).__name__, "ms": (time.time() - started) * 1000}


def pct(values, p):
    if not values:
        return 0
    values = sorted(values)
    idx = min(len(values) - 1, int(round((p / 100.0) * (len(values) - 1))))
    return values[idx]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--concurrency", type=int, default=8)
    ap.add_argument("--total", type=int, default=16)
    ap.add_argument("--question", default="我该挂什么科？")
    args = ap.parse_args()

    token = login()
    results, lock = [], threading.Lock()

    def task(i):
        res = one(token, args.question)
        with lock:
            results.append(res)

    started = time.time()
    with ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        list(pool.map(task, range(args.total)))
    wall = (time.time() - started) * 1000

    ok = [r for r in results if r["status"] == "ok"]
    limited = [r for r in results if r["status"] == "http_429"]
    errors = [r for r in results if r["status"].startswith("error") or
              (r["status"].startswith("http_") and r["status"] != "http_429")]
    lat = [r["ms"] for r in ok]
    degraded = sum(1 for r in ok if r["degraded"])

    print("并发 %d / 总请求 %d / 问题：%s" % (args.concurrency, args.total, args.question))
    print("总耗时 %.0f ms" % wall)
    print("成功 %d | 限流(429) %d | 其它错误 %d | 其中降级 %d" % (len(ok), len(limited), len(errors), degraded))
    if lat:
        print("成功请求延迟：p50=%.0fms p95=%.0fms max=%.0fms" % (pct(lat, 50), pct(lat, 95), max(lat)))
    if limited:
        print("限流请求延迟均值：%.0fms（应远低于正常请求，说明被提前拦截）"
              % statistics.mean([r["ms"] for r in limited]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
