# -*- coding: utf-8 -*-
"""DashScope（OpenAI 兼容）调用，只用标准库。"""
import json
import os
import ssl
import urllib.error
import urllib.request

DEFAULT_BASE = "https://dashscope.aliyuncs.com/compatible-mode/v1"
DEFAULT_MODEL = "qwen-plus"

# Windows 上的 msys Python 常缺 CA 包，按顺序找一个能用的
CA_CANDIDATES = (
    r"C:\Program Files\Git\mingw64\etc\ssl\certs\ca-bundle.crt",
    r"C:\Program Files\Git\usr\ssl\certs\ca-bundle.crt",
    r"C:\msys64\ucrt64\etc\ssl\cert.pem",
)


def ssl_context():
    cafile = os.environ.get("SSL_CERT_FILE")
    if not cafile or not os.path.exists(cafile):
        cafile = next((c for c in CA_CANDIDATES if os.path.exists(c)), None)
    if cafile:
        return ssl.create_default_context(cafile=cafile)
    return ssl.create_default_context()


_SSL_CONTEXT = ssl_context()


class LlmError(Exception):
    pass


def chat(api_key, messages, tools=None, model=None, base_url=None, timeout=25, temperature=0,
         tool_choice="auto"):
    url = (base_url or DEFAULT_BASE).rstrip("/") + "/chat/completions"
    body = {
        "model": model or DEFAULT_MODEL,
        "messages": messages,
        "temperature": temperature,
    }
    if tools:
        body["tools"] = tools
        body["tool_choice"] = tool_choice

    req = urllib.request.Request(
        url,
        data=json.dumps(body, ensure_ascii=False).encode("utf-8"),
        headers={"Content-Type": "application/json", "Authorization": "Bearer " + api_key},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=timeout, context=_SSL_CONTEXT) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        # 把服务端的错误正文带出来，否则只剩 “400 Bad Request”，无法判断原因
        detail = ""
        try:
            detail = e.read().decode("utf-8", "replace")[:300]
        except Exception:  # noqa: BLE001
            pass
        raise LlmError("模型调用失败: HTTP %d %s" % (e.code, detail))
    except Exception as e:  # noqa: BLE001
        raise LlmError("模型调用失败: %s" % e)

    if "error" in payload:
        raise LlmError("模型返回错误: %s" % (payload["error"].get("message") or payload["error"]))
    choices = payload.get("choices") or []
    if not choices:
        raise LlmError("模型没有返回 choices")
    message = choices[0].get("message") or {}
    if not message.get("content") and not message.get("tool_calls"):
        # thinking 模型可能把额度全花在 reasoning_content 上，content 为空
        raise LlmError("模型只输出了推理过程，没有给出结果；请重试或改用非 thinking 模型")
    return message, (payload.get("usage") or {})


def parse_json_block(text):
    """模型有时仍会包一层 Markdown，这里做兜底"""
    if not text:
        return None
    s = text.strip()
    if s.startswith("```"):
        s = s.split("```")[1]
        if s.startswith("json"):
            s = s[4:]
        s = s.strip()
    start, end = s.find("{"), s.rfind("}")
    if start < 0 or end <= start:
        return None
    try:
        return json.loads(s[start:end + 1])
    except Exception:  # noqa: BLE001
        return None
