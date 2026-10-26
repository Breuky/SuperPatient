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

# thinking 模型（如 deepseek-flash）不接受 tool_choice=required，
# 记住一次，后续请求直接走 auto，避免每次都先吃一个 400
_REQUIRED_UNSUPPORTED = set()


def _sanitize(parsed):
    """越界内容直接丢弃：宁可少说，不可给医疗建议"""
    if not parsed:
        return {"summary": "模型没有返回可解析的结果", "highlights": [], "warnings": ["解析失败"]}

    summary = parsed.get("summary") or ""
    if any(w in summary for w in FORBIDDEN):
        # 判病、治疗类的措辞整段换成范围说明，数据由 highlights 继续给
        summary = "这部分属于医学判断，我这边只能提供数据。"

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
    if out_of_scope and not scope_note:
        scope_note = "涉及医学判断，我只提供与它相关的数据"
    if not out_of_scope:
        scope_note = ""

    return {
        "summary": summary,
        "highlights": highlights,
        "warnings": warnings,
        "out_of_scope": out_of_scope,
        "scope_note": scope_note,
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
        "turns": 1,
        "elapsed_ms": int((time.time() - started) * 1000),
        "trace": [],
        "usage": usage,
    })
    return result


def run_agent(api_key, java_base, token, record_id, question, model=None, history=None,
              llm_base=None):
    """工具循环：模型自己决定调用哪些工具、调几次"""
    box = ToolBox(java_base, token)

    # 之前的问答作为上下文；追问（「那上次呢」）必须带上，否则模型不知道指代什么
    messages = [{"role": "system", "content": SYSTEM_AGENT}]
    for turn in (history or [])[-8:]:
        role = turn.get("role")
        content = (turn.get("content") or "").strip()
        if role not in ("user", "assistant") or not content:
            continue
        messages.append({"role": role, "content": content[:600]})

    messages.append({"role": "user",
                     "content": "当前检查记录 id=%s\n问题：%s"
                                % (record_id, question or "这次检查有什么需要关注的？")})

    # 首轮必须查数据；追问允许直接基于上文作答（上文本身来自已验证的工具结果）
    require_tools = not history

    started = time.time()
    turns = 0
    forced = False
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
            result = _sanitize(parse_json_block(msg.get("content")))
            result.update({
                "mode": "agent",
                "turns": turns + 1,
                "elapsed_ms": int((time.time() - started) * 1000),
                "trace": box.trace,
                "forced_finish": forced,
                "no_tool_used": require_tools and not box.trace,
                "usage": usage,
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
        msg, used = chat(api_key, messages, model=model, base_url=llm_base, timeout=15)
        usage = _add_usage(usage, used)
        result = _sanitize(parse_json_block(msg.get("content")))
    except LlmError as e:
        result = {"summary": "生成失败", "highlights": [], "warnings": [str(e)]}
    result.update({
        "mode": "agent",
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
