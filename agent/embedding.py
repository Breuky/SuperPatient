# -*- coding: utf-8 -*-
"""
向量编码（可选层）

两种来源：
  1) API：环境变量 EMBEDDING_API_KEY + EMBEDDING_BASE_URL + EMBEDDING_MODEL
     （OpenAI 兼容的 /embeddings 接口，百炼/硅基/智谱都支持）
  2) 本地：EMBEDDING_LOCAL_DIR 指向 bge-small-zh 的 onnx 目录（需 onnxruntime + tokenizers）

都没有时返回 None，检索自动退回纯 BM25——不报错，不阻断。
"""

from __future__ import annotations

import json
import math
import os

_CACHE = {}


def _normalize(vec):
    norm = math.sqrt(sum(v * v for v in vec)) or 1.0
    return [v / norm for v in vec]


def _api_embed(texts):
    import urllib.request

    key = os.environ.get("EMBEDDING_API_KEY")
    base = (os.environ.get("EMBEDDING_BASE_URL") or "").rstrip("/")
    model = os.environ.get("EMBEDDING_MODEL") or "text-embedding-v3"
    if not key or not base:
        return None

    body = json.dumps({"model": model, "input": texts}, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(base + "/embeddings", data=body, method="POST",
                                 headers={"Content-Type": "application/json",
                                          "Authorization": "Bearer " + key})
    with urllib.request.urlopen(req, timeout=30) as resp:
        payload = json.loads(resp.read().decode("utf-8"))
    items = sorted(payload.get("data") or [], key=lambda x: x.get("index", 0))
    return [_normalize([float(x) for x in it["embedding"]]) for it in items]


def _local_embed(texts):
    import numpy as np          # noqa: F401
    import onnxruntime as ort
    from tokenizers import Tokenizer

    path = os.environ.get("EMBEDDING_LOCAL_DIR")
    if not path:
        return None
    if "session" not in _CACHE:
        _CACHE["session"] = ort.InferenceSession(os.path.join(path, "model.onnx"),
                                                 providers=["CPUExecutionProvider"])
        _CACHE["tokenizer"] = Tokenizer.from_file(os.path.join(path, "tokenizer.json"))
    sess, tok = _CACHE["session"], _CACHE["tokenizer"]

    out = []
    for text in texts:
        enc = tok.encode(text)
        ids = enc.ids[:512]
        feeds = {"input_ids": np.array([ids], dtype=np.int64),
                 "attention_mask": np.ones((1, len(ids)), dtype=np.int64),
                 "token_type_ids": np.zeros((1, len(ids)), dtype=np.int64)}
        names = {i.name for i in sess.get_inputs()}
        feeds = {k: v for k, v in feeds.items() if k in names}
        hidden = sess.run(None, feeds)[0]
        mask = feeds["attention_mask"][0][:, None]
        vec = (hidden[0] * mask).sum(axis=0) / max(1, mask.sum())
        out.append(_normalize([float(x) for x in vec]))
    return out


def embed(texts):
    """返回向量列表；不可用时返回 None"""
    if not texts:
        return []
    for fn in (_api_embed, _local_embed):
        try:
            vecs = fn(texts)
        except Exception:      # noqa: BLE001  依赖缺失/网络失败都当作不可用
            vecs = None
        if vecs:
            return vecs
    return None


def available():
    return embed(["探活"]) is not None
