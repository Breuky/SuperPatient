# -*- coding: utf-8 -*-
"""
Agent 服务（仅标准库，无第三方依赖）。

    python agent/server.py --port 8001

Java 端通过 POST /agent/report-insight 调用；本服务不连数据库，
所有数据都由 tools.py 通过 Java 的只读接口取，鉴权沿用用户的 JWT。
"""
import argparse
import json
import os
import sys
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from agent import run_agent, run_simple  # noqa: E402
from notes import run_followup_notes  # noqa: E402
from llm import LlmError  # noqa: E402

DEFAULT_JAVA_BASE = os.environ.get("JAVA_BASE", "http://localhost:8080")


class AgentServer(ThreadingHTTPServer):
    """禁止端口复用：Windows 下 allow_reuse_address=True 会让第二个实例静默绑定同一个端口，
    导致请求随机落到旧进程（配置不生效、鉴权莫名其妙的 401）。这里改成绑不上就直接报错。"""
    allow_reuse_address = False


class Handler(BaseHTTPRequestHandler):
    service_version = "1.0"

    def log_message(self, fmt, *args):  # noqa: A003
        sys.stderr.write("%s %s\n" % (time.strftime("%H:%M:%S"), fmt % args))

    def _json(self, status, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):  # noqa: N802
        if self.path == "/health":
            self._json(200, {"ok": True, "version": self.service_version})
        else:
            self._json(404, {"ok": False, "error": "not found"})

    def _read_body(self):
        """Spring 的 RestClient 有时用 chunked 编码，没有 Content-Length"""
        if "chunked" in (self.headers.get("Transfer-Encoding") or "").lower():
            chunks = []
            while True:
                line = self.rfile.readline().strip()
                if not line:
                    continue
                size = int(line.split(b";")[0], 16)
                if size == 0:
                    self.rfile.readline()
                    break
                chunks.append(self.rfile.read(size))
                self.rfile.read(2)
            return b"".join(chunks)
        length = int(self.headers.get("Content-Length") or 0)
        return self.rfile.read(length) if length else b""

    def do_POST(self):  # noqa: N802
        raw = self._read_body()
        length = len(raw)
        try:
            body = json.loads(raw.decode("utf-8")) if raw else {}
        except Exception:  # noqa: BLE001
            return self._json(400, {"ok": False, "error": "请求体不是合法 JSON"})

        self.log_message("%s body_keys=%s len=%d", self.path, sorted(body.keys()), length)

        api_key = self.headers.get("X-Llm-Key") or os.environ.get("DASHSCOPE_API_KEY") or ""
        token = self.headers.get("X-User-Token") or ""
        model = self.headers.get("X-Llm-Model") or os.environ.get("LLM_MODEL")
        # 允许换到任意 OpenAI 兼容端点（DeepSeek / 智谱 / 本地 vLLM…）
        llm_base = (self.headers.get("X-Llm-Base")
                    or os.environ.get("AGENT_LLM_BASE_URL")
                    or os.environ.get("LLM_BASE_URL")
                    or None)

        if not api_key:
            return self._json(200, {"ok": False, "error": "缺少模型 Key"})
        # 只记录长度与端点，不落密钥本身
        self.log_message("key_len=%d base=%s model=%s", len(api_key), llm_base or "default", model)

        try:
            if self.path == "/agent/report-insight":
                if not token:
                    return self._json(200, {"ok": False, "error": "缺少用户凭证"})
                java_base = body.get("javaBase") or DEFAULT_JAVA_BASE
                result = run_agent(
                    api_key, java_base, token,
                    body.get("recordId"), body.get("question"), model,
                    body.get("history") or [], llm_base)
            elif self.path == "/agent/summarize":
                result = run_simple(api_key, body.get("evidence") or {}, model, llm_base)
            elif self.path == "/agent/followup-note":
                result = run_followup_notes(api_key, body.get("items") or [], model, llm_base)
            else:
                return self._json(404, {"ok": False, "error": "not found"})
        except LlmError as e:
            return self._json(200, {"ok": False, "error": str(e)})
        except Exception as e:  # noqa: BLE001
            return self._json(200, {"ok": False, "error": "内部错误: %s" % e})

        # agent 模式一次工具都没调，说明在编数据 —— 交给 Java 降级，不能返回幻觉
        if result.get("mode") == "agent" and result.get("no_tool_used"):
            return self._json(200, {"ok": False, "error": "模型未调用任何工具，已拒绝作答"})

        result["ok"] = True
        return self._json(200, result)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--port", type=int, default=8001)
    args = ap.parse_args()
    try:
        server = AgentServer(("127.0.0.1", args.port), Handler)
    except OSError as e:
        print("端口 %d 已被占用（%s）；先关掉旧实例再启动" % (args.port, e))
        raise SystemExit(1)
    print("agent service listening on http://127.0.0.1:%d" % args.port)
    server.serve_forever()


if __name__ == "__main__":
    main()
