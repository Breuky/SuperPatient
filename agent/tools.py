# -*- coding: utf-8 -*-
"""
Agent 可用的只读工具。全部通过 Java 的既有接口取数，不直连数据库。

每个工具都是「模型能自己决定调不调」的原子能力，不是给 prompt 塞数据。
"""
import json
import time
import urllib.parse
import urllib.request


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

    def _list_records(self, limit=10):
        data = self._get("/api/records?page=1&pageSize=%d" % int(limit or 10)) or {}
        rows = data.get("rows") or []
        return [{
            "record_id": r.get("id"),
            "date": (r.get("examDate") or "")[:10],
            "category": r.get("categoryName"),
            "hospital": r.get("hospital"),
            "abnormal_count": r.get("abnormalCount"),
        } for r in rows]

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
