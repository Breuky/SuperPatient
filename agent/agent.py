# -*- coding: utf-8 -*-
"""
两种模式：
  simple —— 调用方（Java）已经组装好证据包，这里只做一次生成，0 次工具调用
  agent  —— 只拿到 recordId 和问题，模型自己决定查什么、查几次
"""
import json
import time

from llm import LlmError, chat, parse_json_block
from prompts import FORBIDDEN, SYSTEM_AGENT, SYSTEM_SIMPLE
from tools import ToolBox

MAX_TURNS = 6
DEADLINE_SECONDS = 20


def _sanitize(parsed):
    """越界内容直接丢弃：宁可少说，不可给医疗建议"""
    if not parsed:
        return {"summary": "模型没有返回可解析的结果", "highlights": [], "warnings": ["解析失败"]}

    summary = parsed.get("summary") or ""
    if any(w in summary for w in FORBIDDEN):
        summary = "本次数据已读取，但生成内容触碰了「不给医疗建议」的边界，已过滤。"

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
    return {"summary": summary, "highlights": highlights, "warnings": warnings}


def run_simple(api_key, evidence, model=None):
    """证据包模式：一次调用，模型没有任何工具"""
    messages = [
        {"role": "system", "content": SYSTEM_SIMPLE},
        {"role": "user", "content": json.dumps(evidence, ensure_ascii=False)},
    ]
    started = time.time()
    msg = chat(api_key, messages, model=model)
    result = _sanitize(parse_json_block(msg.get("content")))
    result.update({
        "mode": "simple",
        "turns": 1,
        "elapsed_ms": int((time.time() - started) * 1000),
        "trace": [],
    })
    return result


def run_agent(api_key, java_base, token, record_id, question, model=None):
    """工具循环：模型自己决定调用哪些工具、调几次"""
    box = ToolBox(java_base, token)
    messages = [
        {"role": "system", "content": SYSTEM_AGENT},
        {"role": "user", "content": "检查记录 id=%s\n问题：%s" % (record_id, question or "这次检查有什么需要关注的？")},
    ]

    started = time.time()
    turns = 0
    forced = False
    nudged = 0
    seen = set()

    while turns < MAX_TURNS and (time.time() - started) < DEADLINE_SECONDS:
        msg = chat(api_key, messages, tools=ToolBox.SCHEMAS, model=model)
        tool_calls = msg.get("tool_calls") or []
        if not tool_calls:
            # 一次工具都没调就想作答 = 凭空编数字，拽回来重试一次
            if not box.trace and nudged < 2:
                nudged += 1
                messages.append({"role": "assistant", "content": msg.get("content") or ""})
                messages.append({"role": "user", "content":
                    "你还没有调用任何工具，手上没有数据。请立刻调用 get_record 等工具取数，不要凭记忆作答。"})
                turns += 1
                continue
            result = _sanitize(parse_json_block(msg.get("content")))
            result.update({
                "mode": "agent",
                "turns": turns + 1,
                "elapsed_ms": int((time.time() - started) * 1000),
                "trace": box.trace,
                "forced_finish": forced,
                "no_tool_used": not box.trace,
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

            messages.append({
                "role": "tool",
                "tool_call_id": call.get("id"),
                "content": box.call(name, args),
            })
        turns += 1

    # 轮数或时间用尽：强制收尾，不让请求无限挂着
    forced = True
    messages.append({"role": "user", "content": "请基于已经取得的工具结果直接输出最终 JSON，不要再调用工具。"})
    try:
        msg = chat(api_key, messages, model=model, timeout=15)
        result = _sanitize(parse_json_block(msg.get("content")))
    except LlmError as e:
        result = {"summary": "生成失败", "highlights": [], "warnings": [str(e)]}
    result.update({
        "mode": "agent",
        "turns": turns,
        "elapsed_ms": int((time.time() - started) * 1000),
        "trace": box.trace,
        "forced_finish": forced,
    })
    return result
