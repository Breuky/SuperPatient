# -*- coding: utf-8 -*-
"""
两种模式：
  simple —— 调用方（Java）已经组装好证据包，这里只做一次生成，0 次工具调用
  agent  —— 只拿到 recordId 和问题，模型自己决定查什么、查几次
"""
import json
import re
import time

from llm import LlmError, chat, parse_json_block
from prompts import FORBIDDEN, SYSTEM_AGENT, SYSTEM_SIMPLE
from scope import (L0_DATA, L1_EXPLAIN, L2_MEDICAL, L3_SERVICE, OFF_TOPIC, OFF_TOPIC_REPLY,
                   OFF_TOPIC_SUGGESTIONS, SERVICE_REPLY, SERVICE_SUGGESTIONS,
                   ceiling_hint, classify, is_transaction)
from tools import ToolBox

MAX_TURNS = 6

# 纯事实问题：答案完全来自工具返回，不需要模型组织语言。
# 命中就走确定性路径，省一次模型调用、也避免模型把日期说错。
FAST_FACTS = (
    ("count", ("一共查过几次", "查过几次", "查了几次", "总共查过", "一共几次", "检查了几次")),
    ("last_date", ("上次检查是什么时候", "最近一次是什么时候", "什么时候查的", "上次是什么时候")),
    ("last_hospital", ("哪家医院", "哪个医院")),
)


def fast_fact(question):
    q = question or ""
    for kind, words in FAST_FACTS:
        if any(w in q for w in words):
            return kind
    return None


def _fast_answer(kind, box):
    """只读一次 list_records，用返回值直接拼答案"""
    data = json.loads(box.call("list_records", {"limit": 1}))
    total = data.get("total")
    records = data.get("records") or []
    first = records[0] if records else {}

    if kind == "count":
        summary = "你一共有 %s 次检查记录。" % (total if total is not None else len(records))
    elif kind == "last_date":
        if not first:
            summary = "还没有检查记录。"
        else:
            summary = "最近一次检查是 %s（%s，%s）。" % (
                first.get("date"), first.get("category") or "未分类", first.get("hospital") or "未填医院")
    else:
        if not first:
            summary = "还没有检查记录。"
        else:
            summary = "最近一次是在%s检查的，日期 %s。" % (
                first.get("hospital") or "未填写医院", first.get("date"))

    return {
        "mode": "agent",
        "capability": L0_DATA,
        "summary": summary,
        "highlights": [],
        "warnings": [],
        "out_of_scope": False,
        "off_topic": False,
        "scope_note": "",
        "suggestions": [],
        "turns": 1,
        "elapsed_ms": 0,
        "trace": box.trace,
        "forced_finish": False,
        "no_tool_used": False,
        "usage": _empty_usage(),
    }
DEADLINE_SECONDS = 20

# thinking 模型（如 deepseek-flash）不接受 tool_choice=required，
# 记住一次，后续请求直接走 auto，避免每次都先吃一个 400
_REQUIRED_UNSUPPORTED = set()


DEFAULT_SCOPE_NOTE = "涉及医学判断，我只提供与它相关的数据"


def _sanitize(parsed, capability=None):
    """越界内容直接丢弃：宁可少说，不可给医疗建议。

    capability 是规则层给出的上限。模型只能在上限内作答，不能自行放宽：
    判成 L2 的问题，无论模型写了什么，正文都替换成范围说明。
    """
    if not parsed:
        return {"summary": "模型没有返回可解析的结果", "highlights": [], "warnings": ["解析失败"]}

    summary = parsed.get("summary") or ""
    if any(w in summary for w in FORBIDDEN):
        # 只丢掉含医疗建议的那一句，保留其余有据可依的内容；
        # 整段替换会把「引用标准原文」也一起打死
        kept = [s for s in re.split(r"(?<=[。！？；])", summary)
                if s.strip() and not any(w in s for w in FORBIDDEN)]
        summary = "".join(kept).strip() or "这部分属于医学判断，我这边只能提供数据。"

    highlights = []
    dropped = 0
    for item in parsed.get("highlights") or []:
        text = (item.get("text") or "") + (item.get("basis") or "")
        if any(w in text for w in FORBIDDEN):
            dropped += 1
            continue
        highlights.append({
            "indicator": item.get("indicator"),
            "text": item.get("text"),
            "basis": item.get("basis"),
        })

    warnings = list(parsed.get("warnings") or [])
    if dropped:
        warnings.append("有 %d 条内容因包含医疗建议类表述被过滤" % dropped)

    # 越界提问：模型声明「这是医学判断」，前端单独展示，不混在正文里
    scope_note = (parsed.get("scope_note") or "").strip()
    if any(w in scope_note for w in FORBIDDEN):
        scope_note = ""

    # 模型偶尔只在正文里声明、忘了置位；用固定标记兜住
    marker = "【医学判断】"
    out_of_scope = bool(parsed.get("out_of_scope")) or summary.startswith(marker)
    if summary.startswith(marker):
        summary = summary[len(marker):].strip()
    # 规则层封顶：模型自己没置位也要按 L2 处理，防止「说得很像数据结论」
    if capability == L2_MEDICAL:
        out_of_scope = True
        summary = "这属于医学判断，我这边只能提供数据。"
    if out_of_scope and not scope_note:
        scope_note = DEFAULT_SCOPE_NOTE
    if not out_of_scope:
        scope_note = ""

    return {
        "summary": summary,
        "highlights": highlights,
        "warnings": warnings,
        "out_of_scope": out_of_scope,
        "scope_note": scope_note,
        "off_topic": bool(parsed.get("off_topic")),
    }


def run_simple(api_key, evidence, model=None, llm_base=None):
    """证据包模式：一次调用，模型没有任何工具"""
    messages = [
        {"role": "system", "content": SYSTEM_SIMPLE},
        {"role": "user", "content": json.dumps(evidence, ensure_ascii=False)},
    ]
    started = time.time()
    msg, usage = chat(api_key, messages, model=model, base_url=llm_base)
    result = _sanitize(parse_json_block(msg.get("content")))
    result.update({
        "mode": "simple",
        "capability": L1_EXPLAIN,
        "turns": 1,
        "elapsed_ms": int((time.time() - started) * 1000),
        "trace": [],
        "usage": usage,
    })
    return result


def run_agent(api_key, java_base, token, record_id, question, model=None, history=None,
              llm_base=None):
    """工具循环：模型自己决定调用哪些工具、调几次"""
    capability = classify(question)

    # 与检查数据无关的问题：不调模型、不查数据，直接说清楚能答什么
    if capability == OFF_TOPIC:
        return {
            "mode": "agent",
            "summary": OFF_TOPIC_REPLY,
            "highlights": [],
            "warnings": [],
            "suggestions": list(OFF_TOPIC_SUGGESTIONS),
            "off_topic": True,
            "turns": 0,
            "elapsed_ms": 0,
            "trace": [],
            "usage": _empty_usage(),
            "forced_finish": False,
            "no_tool_used": False,
            "capability": OFF_TOPIC,
        }

    # 只有「替我挂号/问诊」这类代办诉求才短路；「该挂什么科」要走工具给白名单候选
    if capability == L3_SERVICE and is_transaction(question):
        return {
            "mode": "agent",
            "summary": SERVICE_REPLY,
            "highlights": [],
            "warnings": [],
            "suggestions": list(SERVICE_SUGGESTIONS),
            "off_topic": False,
            "out_of_scope": False,
            "turns": 0,
            "elapsed_ms": 0,
            "trace": [],
            "usage": _empty_usage(),
            "forced_finish": False,
            "no_tool_used": False,
            "capability": L3_SERVICE,
        }

    # L0 纯事实问题：走确定性路径，不调模型
    if capability == L0_DATA:
        kind = fast_fact(question)
        if kind:
            try:
                return _fast_answer(kind, ToolBox(java_base, token))
            except Exception as e:      # noqa: BLE001
                log.warning("快路径失败，回落模型: %s", e)

    box = ToolBox(java_base, token)

    # 之前的问答作为上下文；追问（「那上次呢」）必须带上，否则模型不知道指代什么
    messages = [{"role": "system", "content": SYSTEM_AGENT}]
    history = list(history or [])
    if len(history) > 6:
        # 只保留最近 4 轮原文，更早的压成一段要点（抽取式，不调模型）
        older, history = history[:-4], history[-4:]
        brief = "；".join("%s：%s" % (t.get("role"), (t.get("content") or "")[:80])
                          for t in older[-4:] if t.get("content"))
        messages.append({"role": "user", "content": "（此前对话要点，仅供参考）" + brief})

    for turn in history[-8:]:
        role = turn.get("role")
        content = (turn.get("content") or "").strip()
        if role not in ("user", "assistant") or not content:
            continue
        messages.append({"role": role, "content": content[:600]})

    if record_id:
        context = "当前检查记录 id=%s（用户在记录详情页提问）" % record_id
    else:
        context = "当前没有指定记录（用户在全局助手提问，范围是全部记录）"
    messages.append({"role": "user",
                     "content": "%s\n【能力上限】%s\n问题：%s" % (
                         context, ceiling_hint(capability),
                         question or "我最近的检查有什么需要关注的？")})

    # 首轮必须查数据；追问允许直接基于上文作答（上文本身来自已验证的工具结果）
    require_tools = not history

    started = time.time()
    turns = 0
    forced = False
    departments_payload = None
    hospitals_payload = None
    knowledge_payload = None
    knowledge_rank = -1
    knowledge_empty = False
    nudged = 0
    seen = set()
    usage = _empty_usage()

    while turns < MAX_TURNS and (time.time() - started) < DEADLINE_SECONDS:
        # 首轮还没取到任何数据时强制调用工具：不依赖模型"愿不愿意查"，
        # 也就不需要在提示词里写「哪类问题用哪个工具」
        choice = "required" if (require_tools and not box.trace
                               and (model or "") not in _REQUIRED_UNSUPPORTED) else "auto"
        try:
            msg, used = chat(api_key, messages, tools=ToolBox.SCHEMAS, model=model,
                             base_url=llm_base, tool_choice=choice)
        except LlmError as e:
            # 个别 OpenAI 兼容端点不支持 tool_choice=required，退回 auto 再试一次
            if choice == "required" and "tool_choice" in str(e).lower():
                _REQUIRED_UNSUPPORTED.add(model or "")
                msg, used = chat(api_key, messages, tools=ToolBox.SCHEMAS, model=model,
                                 base_url=llm_base, tool_choice="auto")
            else:
                raise
        usage = _add_usage(usage, used)
        tool_calls = msg.get("tool_calls") or []
        if not tool_calls:
            # 首轮一次工具都没调就想作答 = 凭空编数字，拽回来重试一次
            if require_tools and not box.trace and nudged < 2:
                nudged += 1
                messages.append({"role": "assistant", "content": msg.get("content") or ""})
                messages.append({"role": "user", "content":
                    "你还没有调用任何工具，手上没有数据。请立刻调用 get_record 等工具取数，不要凭记忆作答。"})
                turns += 1
                continue
            result = _sanitize(parse_json_block(msg.get("content")), capability)
            result.update({
                "mode": "agent",
                "capability": capability,
                "turns": turns + 1,
                "elapsed_ms": int((time.time() - started) * 1000),
                "trace": box.trace,
                "forced_finish": forced,
                "no_tool_used": require_tools and not box.trace,
                "usage": usage,
                "triage": departments_payload,
                "hospitals": hospitals_payload,
                "knowledge": knowledge_payload,
            })
            return result

        messages.append({
            "role": "assistant",
            "content": msg.get("content") or "",
            "tool_calls": tool_calls,
        })
        for call in tool_calls:
            fn = call.get("function") or {}
            name = fn.get("name")
            try:
                args = json.loads(fn.get("arguments") or "{}")
            except Exception:  # noqa: BLE001
                args = {}

            if name == "search_knowledge" and knowledge_empty:
                messages.append({
                    "role": "tool",
                    "tool_call_id": call.get("id"),
                    "content": json.dumps({"hits": [],
                                           "note": "知识库已确认没有相关条目，不要重试，直接说明没有这条"},
                                          ensure_ascii=False),
                })
                continue

            # 同样的调用已经做过 → 不再打接口，直接提醒模型换策略
            signature = name + ":" + json.dumps(args, sort_keys=True, ensure_ascii=False)
            if signature in seen:
                messages.append({
                    "role": "tool",
                    "tool_call_id": call.get("id"),
                    "content": json.dumps({"error": "这次调用与之前完全相同，结果不会变化，请改用其他工具或参数"},
                                          ensure_ascii=False),
                })
                continue
            seen.add(signature)

            content = box.call(name, args)
            messages.append({
                "role": "tool",
                "tool_call_id": call.get("id"),
                "content": content,
            })
            # 导诊结果单独带出去，前端渲染成卡片（模型只在正文里转述）
            if name == "suggest_departments":
                try:
                    parsed = json.loads(content)
                    if parsed.get("departments"):
                        departments_payload = parsed
                except Exception:  # noqa: BLE001
                    pass
            elif name == "search_knowledge":
                # 以后一次检索为准：模型可能先查一次空、再换个词查；
                # 若只保留「有命中」的那次，界面会出现与最终回答不一致的引用
                try:
                    parsed = json.loads(content)
                    if isinstance(parsed, dict):
                        # 带意图（fields 非空）的那次检索更可信：
                        # 模型可能先按原问题查空、再换个泛词乱查，后者不该覆盖前者
                        rank = 1 if ((parsed.get("query") or {}).get("fields")) else 0
                        if rank >= knowledge_rank:
                            knowledge_payload, knowledge_rank = parsed, rank
                        if not (parsed.get("hits") or []):
                            knowledge_empty = True
                except Exception:  # noqa: BLE001
                    pass
            elif name == "find_hospitals":
                try:
                    parsed = json.loads(content)
                    if parsed.get("hospitals"):
                        hospitals_payload = parsed
                except Exception:  # noqa: BLE001
                    pass
        turns += 1

    # 轮数或时间用尽：强制收尾，不让请求无限挂着
    forced = True
    messages.append({"role": "user", "content": "请基于已经取得的工具结果直接输出最终 JSON，不要再调用工具。"})
    try:
        msg, used = chat(api_key, messages, model=model, base_url=llm_base, timeout=15)
        usage = _add_usage(usage, used)
        result = _sanitize(parse_json_block(msg.get("content")), capability)
    except LlmError as e:
        result = {"summary": "生成失败", "highlights": [], "warnings": [str(e)]}
    result.update({
        "mode": "agent",
        "capability": capability,
        "triage": departments_payload,
        "hospitals": hospitals_payload,
                "knowledge": knowledge_payload,
        "turns": turns,
        "elapsed_ms": int((time.time() - started) * 1000),
        "trace": box.trace,
        "forced_finish": forced,
        "usage": usage,
    })
    return result


def _empty_usage():
    return {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0}


def _add_usage(total, used):
    if not used:
        return total
    return {
        "prompt_tokens": total["prompt_tokens"] + int(used.get("prompt_tokens") or 0),
        "completion_tokens": total["completion_tokens"] + int(used.get("completion_tokens") or 0),
        "total_tokens": total["total_tokens"] + int(used.get("total_tokens") or 0),
    }