# -*- coding: utf-8 -*-
"""
知识条目检索（RAG 的检索层）

设计取舍
1. 语料存在 MySQL，Agent 不直连数据库：启动/首次调用时从 Java 的 /api/knowledge 拉全量，
   进程内建 BM25 索引并缓存。条数少（百级），内存完全够。
2. 中文不装分词器：字符二元组（bigram）+ 单字兜底，零依赖、结果可复现。
3. 查询规范化只用规则：清洗 → 指标锚定（用指标别名）→ 意图路由到 field。
   不让模型改写 query：漏召回可以接受，改错意思不行。
4. 低于阈值就返回空，宁可说「知识库没有」，也不拿相邻条目凑数。
"""

from __future__ import annotations

import math
import re
import time

K1 = 1.5
B = 0.75
MIN_SCORE = 0.15          # 归一化后的最低分
# 来源等级权重：同等相关时优先国标/行标，其次指南共识
SOURCE_BOOST = {"A": 1.15, "B": 1.08, "C": 1.03, "D": 0.95}
CACHE_TTL_SECONDS = 300

# 问题意图 → 知识条目类型
FIELD_HINTS = {
    "prep": ("空腹", "准备", "注意", "要求", "前一天", "采血前", "抽血前"),
    "compare": ("区别", "差别", "哪个更", "相比", "对比"),
    "why_vary": ("为什么", "区间", "范围", "不一样", "不同", "各家", "医院之间"),
    "unit_note": ("单位", "换算", "mg/dl", "mmol", "μmol"),
    "report_note": ("箭头", "符号", "偏高偏低", "↑", "↓"),
    "interference": ("影响", "干扰", "因素", "会不会影响", "吃药", "运动", "饮食"),
    "definition": ("是什么", "什么意思", "代表", "有什么用", "作用", "定义"),
}

# 只保留中英文数字，其余当分隔符
_CLEAN = re.compile(r"[^0-9a-zA-Z\u4e00-\u9fff]+")
_FILLER = ("帮我看看", "我想问", "想问一下", "请问", "麻烦", "我的", "我呢", "啊", "呢", "吧", "吗")


def clean(text: str) -> str:
    t = (text or "").strip().lower()
    for w in _FILLER:
        t = t.replace(w, " ")
    t = _CLEAN.sub(" ", t)
    return re.sub(r"\s+", " ", t).strip()


def tokenize(text: str):
    """中文按二元组切，英文/数字按整词；单字也保留一份（"钾"这类单字指标）"""
    tokens = []
    for chunk in clean(text).split(" "):
        if not chunk:
            continue
        if re.fullmatch(r"[0-9a-z.\-]+", chunk):
            tokens.append(chunk)
            continue
        if len(chunk) == 1:
            tokens.append(chunk)
            continue
        tokens.extend(chunk[i:i + 2] for i in range(len(chunk) - 1))
        tokens.extend(chunk)          # 单字兜底
    return tokens


def normalize(query: str, indicators):
    """
    indicators: [{"id":1,"name":"血红蛋白","aliases":"Hb|HGB"}]
    返回 (cleaned, anchored_ids, field_weights)
    """
    cleaned = clean(query)
    flat = cleaned.replace(" ", "")

    anchored = []
    for ind in indicators or ():
        names = [ind.get("name") or ""]
        names += [a for a in (ind.get("aliases") or "").split("|") if a]
        for name in names:
            key = clean(name).replace(" ", "")
            if key and (key in flat or (len(key) >= 3 and key[:2] in flat)):
                if ind.get("id") not in anchored:
                    anchored.append(ind.get("id"))
                break

    fields = []
    lowered = (query or "").lower()
    for field, words in FIELD_HINTS.items():
        if any(w in lowered for w in words):
            fields.append(field)
    return cleaned, anchored, fields


def cosine(a, b):
    return sum(x * y for x, y in zip(a, b))          # 已归一化，点积即余弦


def rrf(rank_lists, k=60):
    """倒数排名融合：多路召回各给一个名次，按 1/(k+rank) 累加。"""
    scores = {}
    for ranks in rank_lists:
        for pos, idx in enumerate(ranks):
            scores[idx] = scores.get(idx, 0.0) + 1.0 / (k + pos + 1)
    return scores


class Index:
    def __init__(self, entries, vectors=None):
        self.entries = [e for e in entries if (e.get("text") or "").strip()]
        self.docs = []
        self.df = {}
        for e in self.entries:
            body = " ".join(filter(None, [e.get("indicatorName"), e.get("aliases"),
                                          e.get("text")]))
            tokens = tokenize(body)
            tf = {}
            for t in tokens:
                tf[t] = tf.get(t, 0) + 1
            self.docs.append({"entry": e, "tf": tf, "len": len(tokens) or 1})
            for t in tf:
                self.df[t] = self.df.get(t, 0) + 1

        self.n = len(self.docs)
        self.avg_len = (sum(d["len"] for d in self.docs) / self.n) if self.n else 1.0
        # 向量层是可选的：没装 embedding 时保持 None，检索退回纯 BM25
        self.vectors = vectors if vectors and len(vectors) == self.n else None

    def _idf(self, token):
        df = self.df.get(token, 0)
        return math.log(1 + (self.n - df + 0.5) / (df + 0.5)) if df else 0.0

    def search(self, query, indicators=None, top_k=3, min_score=MIN_SCORE, query_vector=None):
        if not self.n:
            return {"hits": [], "note": "知识库为空", "query": {"cleaned": clean(query)}}

        cleaned, anchored, fields = normalize(query, indicators)
        tokens = tokenize(cleaned)
        if not tokens:
            return {"hits": [], "note": "问题太短，无法检索", "query": {"cleaned": cleaned}}

        idf_sum = sum(self._idf(t) for t in set(tokens)) or 1.0
        scored = []
        for pos, doc in enumerate(self.docs):
            entry = doc["entry"]
            raw = 0.0
            for t in set(tokens):
                tf = doc["tf"].get(t, 0)
                if not tf:
                    continue
                denom = tf + K1 * (1 - B + B * doc["len"] / self.avg_len)
                raw += self._idf(t) * (tf * (K1 + 1)) / denom
            if not raw:
                continue

            boosted = raw
            if entry.get("field") in fields:
                boosted *= 1.5
            boosted *= SOURCE_BOOST.get(entry.get("originLevel") or "", 1.0)
            if anchored:
                if entry.get("indicatorId") in anchored:
                    boosted *= 2.0
                elif entry.get("indicatorId") is None:
                    boosted *= 1.2
                else:
                    boosted *= 0.2

            score = boosted / idf_sum
            if score >= min_score:
                scored.append((score, pos))

        if not scored:
            note = ("知识库里没有「%s」类条目" % "/".join(fields)) if fields \
                else "知识库里没有与这个问题匹配的条目"
            return {"hits": [], "note": note,
                    "query": {"cleaned": cleaned, "anchored": anchored, "fields": fields}}

        scored.sort(key=lambda x: -x[0])

        # 混合检索：BM25 名次 + 向量名次，RRF 融合
        if self.vectors and query_vector:
            bm25_order = [pos for _, pos in scored]
            sims = sorted(((cosine(query_vector, self.vectors[i]), i)
                           for i in range(len(self.docs))), reverse=True)
            vec_order = [i for s, i in sims if s > 0]
            fused = rrf([bm25_order, vec_order])
            scored = sorted(((fused[pos], pos) for _, pos in scored), key=lambda x: -x[0])

        # 意图硬过滤：问「是什么」就只看 definition 条目。
        # 该类型一条都没有时返回空，而不是拿相近条目凑——凑出来的引用比没有更糟。
        if fields:
            same_field = [(s, pos) for s, pos in scored if self.docs[pos]["entry"].get("field") in fields]
            if same_field:
                scored = same_field
            else:
                return {"hits": [], "note": "知识库里没有「%s」类条目" % "/".join(fields),
                        "query": {"cleaned": cleaned, "anchored": anchored, "fields": fields}}

        hits = [{
            "score": round(s, 3),
            "text": self.docs[pos]["entry"].get("text"),
            "field": self.docs[pos]["entry"].get("field"),
            "indicator": self.docs[pos]["entry"].get("indicatorName"),
            "source_id": self.docs[pos]["entry"].get("sourceId"),
            "source_title": self.docs[pos]["entry"].get("sourceTitle"),
            "locator": self.docs[pos]["entry"].get("locator"),
        } for s, pos in scored[:top_k]]

        return {
            "hits": hits,
            "note": "" if hits else "知识库里没有与这个问题匹配的条目",
            "query": {"cleaned": cleaned, "anchored": anchored, "fields": fields},
        }


_cache = {"at": 0.0, "index": None}


def build_index(entries, vectors=None):
    _cache["index"] = Index(entries, vectors)
    _cache["at"] = time.time()
    return _cache["index"]


def get_index(loader, embedder=None):
    """loader 返回 (entries, indicators)；带 TTL 缓存，避免每次问答都拉全量"""
    if _cache["index"] is not None and time.time() - _cache["at"] < CACHE_TTL_SECONDS:
        return _cache["index"]
    entries, indicators = loader()
    vectors = None
    if embedder is not None:
        texts = [" ".join(filter(None, [e.get("indicatorName"), e.get("aliases"), e.get("text")]))
                 for e in entries]
        vectors = embedder(texts)
    idx = build_index(entries, vectors)
    idx.indicators = indicators
    return idx