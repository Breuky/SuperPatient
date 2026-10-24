# -*- coding: utf-8 -*-
"""
批量调用 /api/parse/report，把 5 张样本的抽取结果存到 raw/ 目录。
改了 prompt / 别名 / 指标库之后，重新跑一次再用 run_eval.py 对比。

用法:
    python eval/extract_all.py --out raw3
    python eval/extract_all.py --out raw3 --base http://localhost:8080 --user demo --password demo1234
"""
import argparse
import io
import json
import os
import subprocess
import sys

CURL = "curl.exe" if os.name == "nt" else "curl"


def curl(args):
    p = subprocess.run([CURL] + args, capture_output=True)
    return p.stdout.decode("utf-8", errors="replace")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://localhost:8081")
    ap.add_argument("--user", default="demo")
    ap.add_argument("--password", default="demo1234")
    ap.add_argument("--samples", default="samples")
    ap.add_argument("--out", default="raw3")
    ap.add_argument("--category", type=int, default=1)
    ap.add_argument("--only", default="", help="只跑指定样本，逗号分隔，如 06,07")
    args = ap.parse_args()

    root = os.path.dirname(os.path.abspath(__file__))
    sample_dir = os.path.join(root, args.samples)
    out_dir = os.path.join(root, args.out)
    os.makedirs(out_dir, exist_ok=True)

    login_body = json.dumps({"username": args.user, "password": args.password})
    resp = curl(["-s", "-X", "POST", args.base + "/api/auth/login",
                 "-H", "Content-Type: application/json", "-d", login_body])
    token = json.loads(resp)["data"]["token"]

    only = [s.strip() for s in args.only.split(",") if s.strip()]

    for fn in sorted(os.listdir(sample_dir)):
        if not fn.lower().endswith((".jpg", ".jpeg", ".png")):
            continue
        if only and os.path.splitext(fn)[0] not in only:
            continue
        path = os.path.join(sample_dir, fn)
        up = curl(["-s", "-X", "POST", args.base + "/api/files/upload",
                   "-H", "token: " + token, "-F", "file=@" + path, "-F", "type=report"])
        url = json.loads(up)["data"]["url"]
        body = json.dumps({"imageUrl": url, "categoryId": args.category})
        raw = curl(["-s", "-m", "300", "-X", "POST", args.base + "/api/parse/report",
                    "-H", "token: " + token,
                    "-H", "Content-Type: application/json", "-d", body])
        name = os.path.splitext(fn)[0] + ".json"
        io.open(os.path.join(out_dir, name), "w", encoding="utf-8").write(raw)
        data = json.loads(raw)
        if data.get("code") == 1:
            d = data["data"]
            print("%s  匹配 %d / 未匹配 %d" % (name, len(d["candidates"]), len(d["unmatched"])))
        else:
            print("%s  失败: %s" % (name, data.get("msg")))


if __name__ == "__main__":
    main()
