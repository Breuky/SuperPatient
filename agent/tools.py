# -*- coding: utf-8 -*-
"""
Agent 可用的只读工具。全部通过 Java 的既有接口取数，不直连数据库。

每个工具都是「模型能自己决定调不调」的原子能力，不是给 prompt 塞数据。
"""
import json
import time
import urllib.parse
import urllib.request

import embedding
import knowledge
from departments import suggest as suggest_departments


class ToolError(Exception):
    """工具执行失败，会把原因回给模型让它换策略"""


class ToolBox:
    def __init__(self, java_base, token, timeout=8):
        self.java_base = java_base.rstrip("/")
        self.token = token
        self.timeout = timeout
        self.trace = []

    # ---------- 工具定义（给模型看） ----------
    SCHEMAS = [
        {
            "type": "function",
            "function": {
                "name": "find_indicator",
                "description": "按关键词模糊查找指标（如「血脂」「血红蛋白」），返回指标 id、名称、单位。用户提到笼统说法时先用它确认具体指标。",
                "parameters": {
                    "type": "object",
                    "properties": {"keyword": {"type": "string", "description": "指标名或其中一部分"}},
                    "required": ["keyword"],
                },
            },
        },
        {
            "type": "function",
            "function": {
                "name": "overview",
                "description": "一次拿到当前用户全部记录的宏观统计：总次数、时间范围、各分类次数、异常最多的指标。问「一共查过几次」「哪项异常最多」「最早/最近一次」时先用它，不要靠 list_records 数条数。",
                "parameters": {"type": "object", "properties": {}, "required": []},
            },
        },
        {
            "type": "function",
            "function": {
                "name": "get_record",
                "description": "取某次检查的全部指标值、单位、参考区间与异常标记。问「这次/本次」的异常项、具体数值时先用它。",
                "parameters": {
                    "type": "object",
                    "properties": {"record_id": {"type": "integer"}},
                    "required": ["record_id"],
                },
            },
        },
        {
            "type": "function",
            "function": {
                "name": "list_records",
                "description": "列出最近的检查记录（日期、类型、医院、异常项数）。问「上次检查」或需要找某次记录 id 时先调它。",
                "parameters": {
                    "type": "object",
                    "properties": {"limit": {"type": "integer", "description": "最多几条，默认 10"}},
                },
            },
        },
        {
            "type": "function",
            "function": {
                "name": "get_trend",
                "description": "取某个指标的历史数值序列（按时间升序）。问「比上次」「变化」「最近怎么样」时用它。",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "indicator_id": {"type": "integer"},
                        "limit": {"type": "integer", "description": "最多返回几条，默认 8"},
                    },
                    "required": ["indicator_id"],
                },
            },
        },
        {
            "type": "function",
            "function": {
                "name": "get_reference_rules",
                "description": "取某个指标的性别/年龄参考区间规则，用于解释「为什么这次的正常范围和以前不同」。",
                "parameters": {
                    "type": "object",
                    "properties": {"indicator_id": {"type": "integer"}},
                    "required": ["indicator_id"],
                },
            },
        },
        {
            "type": "function",
            "function": {
                "name": "suggest_departments",
                "description": "根据用户历史异常指标推荐该去哪个科室（纯规则映射，不涉及诊断）。"
                               "问「该挂什么科」「看哪个科室」「需要去医院吗」时用它。"
                               "返回候选科室 + 依据 + 依据的异常指标，不要自己编科室名。",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "limit": {"type": "integer", "description": "参考多少条异常指标，默认 10"},
                    },
                },
            },
        },
        {
            "type": "function",
            "function": {
                "name": "find_hospitals",
                "description": "按城市 + 科室查白名单内的医院（只读、结果来自本地白名单）。"
                               "用户没说城市时按杭州查，并在回答里说明这是默认假设。"
                               "先调 suggest_departments 拿到科室，再用它查医院；"
                               "不要自己编医院名，也不要承诺号源。",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "city": {"type": "string", "description": "城市，默认杭州"},
                        "department": {"type": "string", "description": "科室名，如内分泌科"},
                        "limit": {"type": "integer", "description": "最多几家，默认 5"},
                    },
                },
            },
        },
        {
            "type": "function",
            "function": {
                "name": "search_knowledge",
                "description": "检索指标知识条目（是什么、为什么区间不同、检测前准备、影响因素、单位）。"
                               "只返回带出处的条目；没有命中就返回空，不要用常识补。"
                               "回答时必须写出处（来源 + 定位），且不能把通用知识说成对用户个人的结论。",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "query": {"type": "string", "description": "检索词，通常是用户原问题"},
                        "top_k": {"type": "integer", "description": "最多几条，默认 3"},
                    },
                    "required": ["query"],
                },
            },
        },
    ]

    # ---------- 执行 ----------
    def call(self, name, args):
        """执行工具并记录 trace；异常转成可读文本回给模型，不中断循环"""
        started = time.time()
        ok = True
        try:
            result = getattr(self, "_" + name)(**(args or {}))
        except TypeError as e:
            ok = False
            result = {"error": "参数不合法: %s" % e}
        except ToolError as e:
            ok = False
            result = {"error": str(e)}
        except Exception as e:  # noqa: BLE001
            ok = False
            result = {"error": "工具执行失败: %s" % e}
        self.trace.append({
            "tool": name, "args": args or {},
            "ms": int((time.time() - started) * 1000), "ok": ok,
        })
        return json.dumps(result, ensure_ascii=False)

    def _get(self, path):
        req = urllib.request.Request(self.java_base + path, headers={"token": self.token})
        try:
            with urllib.request.urlopen(req, timeout=self.timeout) as resp:
                body = json.loads(resp.read().decode("utf-8"))
        except Exception as e:  # noqa: BLE001
            raise ToolError("接口调用失败: %s" % e)
        if body.get("code") != 1:
            raise ToolError(body.get("msg") or "接口返回失败")
        return body.get("data")

    def _find_indicator(self, keyword=""):
        """同时查「当前可选指标」和「用户有记录但可能已停用的指标」，
        否则停用项的 id 找不到，模型只能瞎猜。"""
        key = (keyword or "").strip()
        if not key:
            return []

        hits = {}
        for i in (self._get("/api/indicators") or []):
            if key in (i.get("name") or "") or key in (i.get("aliases") or ""):
                hits[i["id"]] = {"indicator_id": i["id"], "name": i.get("name"),
                                 "unit": i.get("unit"), "has_history": False}
        for i in (self._get("/api/trendable-indicators") or []):
            name = i.get("indicatorName") or ""
            if key in name and i.get("indicatorId") not in hits:
                hits[i["indicatorId"]] = {"indicator_id": i["indicatorId"], "name": name,
                                          "unit": i.get("unit"), "has_history": True}
            elif i.get("indicatorId") in hits:
                hits[i["indicatorId"]]["has_history"] = True
        return list(hits.values())[:10]

    def _overview(self):
        return self._get("/api/statistics/overview") or {}

    def _list_records(self, limit=10):
        data = self._get("/api/records?page=1&pageSize=%d" % int(limit or 10)) or {}
        rows = data.get("rows") or []
        return {
            # total 是全量条数，回答「一共几次检查」必须用它，不能数当前页
            "total": data.get("total"),
            "returned": len(rows),
            "records": [{
                "record_id": r.get("id"),
                "date": (r.get("examDate") or "")[:10],
                "category": r.get("categoryName"),
                "hospital": r.get("hospital"),
                "abnormal_count": r.get("abnormalCount"),
            } for r in rows],
        }

    def _get_record(self, record_id=None):
        if not record_id:
            raise ToolError("record_id 不能为空")
        data = self._get("/api/records/%s" % int(record_id)) or {}
        values = []
        for v in data.get("values", []):
            values.append({
                "indicator": v.get("indicatorName"),
                "value": v.get("valueDecimal") if v.get("valueDecimal") is not None else v.get("valueText"),
                "unit": v.get("unit"),
                "ref_min": v.get("refMin"),
                "ref_max": v.get("refMax"),
                "ref_text": v.get("refText"),
                "is_abnormal": v.get("isAbnormal"),
            })
        return {
            "record_id": data.get("id"),
            "exam_date": data.get("examDate"),
            "hospital": data.get("hospital"),
            "category": data.get("categoryName"),
            "values": values,
        }

    def _get_trend(self, indicator_id=None, limit=8):
        if not indicator_id:
            raise ToolError("indicator_id 不能为空")
        data = self._get("/api/trends?indicatorId=%s" % int(indicator_id)) or {}
        points = data.get("points", [])
        points = points[-int(limit):] if limit else points
        return {
            "indicator": (data.get("indicator") or {}).get("name"),
            "unit": (data.get("indicator") or {}).get("unit"),
            "points": [{"date": p.get("examDate"), "value": p.get("value"), "is_abnormal": p.get("isAbnormal")}
                       for p in points],
        }

    def _get_reference_rules(self, indicator_id=None):
        if not indicator_id:
            raise ToolError("indicator_id 不能为空")
        data = self._get("/api/indicators/%s/reference-ranges" % int(indicator_id)) or []
        return [{"gender": r.get("gender"), "age_min": r.get("ageMin"), "age_max": r.get("ageMax"),
                 "ref_min": r.get("refMin"), "ref_max": r.get("refMax"), "source": r.get("source")}
                for r in data]

    def _search_knowledge(self, query=None, top_k=3):
        """检索知识条目：索引在进程内，语料来自 Java 的 /api/knowledge"""
        if not (query or "").strip():
            raise ToolError("query 不能为空")

        def loader():
            entries = self._get("/api/knowledge") or []
            indicators = self._get("/api/indicators") or []
            return entries, indicators

        idx = knowledge.get_index(loader, embedder=embedding.embed)
        query_vector = None
        if getattr(idx, "vectors", None):
            vecs = embedding.embed([query])
            query_vector = vecs[0] if vecs else None
        return idx.search(query, getattr(idx, "indicators", []),
                          top_k=max(1, int(top_k or 3)), query_vector=query_vector)

    def _find_hospitals(self, city="杭州", department=None, limit=5):
        """白名单查询：只返回库里有的医院，模型不能新增"""
        query = ["limit=%d" % max(1, int(limit or 5))]
        if city:
            query.append("city=" + urllib.parse.quote(city))
        if department:
            query.append("department=" + urllib.parse.quote(department))
        data = self._get("/api/hospitals?" + "&".join(query)) or []
        if not data:
            return {"hospitals": [],
                    "note": "该城市/科室暂无已录入的白名单医院"}
        return {
            "hospitals": [{
                "name": h.get("name"), "level": h.get("level"),
                "hospital_type": h.get("hospitalType"),
                "department": h.get("department"),
                "specialty_level": h.get("specialtyLevel"),
                "address": h.get("address"),
                "booking_url": h.get("bookingUrl"),
                "verified": bool(h.get("verified")),
                "department_verified": bool(h.get("departmentVerified")),
            } for h in data],
            "note": "仅来自白名单。verified=医院等级已核对；department_verified=该科室资质已核对。"
                    "未核对的不要说成「最好」，也不要承诺号源",
        }

    def _suggest_departments(self, limit=10):
        """按历史异常指标推科室：指标来自接口，映射来自本地规则表，全程不调模型"""
        data = self._get("/api/statistics/overview") or {}
        raw = data.get("topAbnormal") or []
        # 同一指标可能存在公共/私有两条（id 不同、名字相同），按名字去重，次数取最大
        seen_names, deduped = set(), []
        for item in raw:
            name = (item.get("name") or "").strip()
            if not name or name in seen_names:
                continue
            seen_names.add(name)
            deduped.append(item)
        top = deduped[: max(1, int(limit or 10))]
        if not top:
            return {"departments": [], "based_on": [],
                    "note": "没有异常记录，无法按指标推荐科室"}

        mapped = suggest_departments([{"name": x.get("name")} for x in top])
        return {
            "based_on": [{"name": x.get("name"), "unit": x.get("unit"),
                          "times": x.get("times")} for x in top],
            "departments": mapped["departments"],
            "unmapped": mapped["unmapped"],
            "note": "推荐仅基于历史异常指标，不构成诊断；具体以医院分诊为准",
        }