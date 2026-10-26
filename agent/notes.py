# -*- coding: utf-8 -*-
"""随访提醒的文案润色：把规则生成的 reason 说得像人话。不做判断，只改说法。"""
import json

from llm import chat, parse_json_block
from prompts import FORBIDDEN

SYSTEM_NOTE = """你是文案润色助手。输入是若干条「随访提醒」的事实数据，请为每条写一句中文说明。

要求：
- 只能使用给定数据里的信息（指标名、次数、方向、间隔天数、上次日期、到期日、超期天数）
- 一句话，40 字以内，口语化，不要罗列数字
- 不要出现：建议、应该、需要、治疗、用药、剂量、确诊、就医、复查（这些属于医疗指令）
- 不要判断病情轻重

输出 JSON：{"notes":[{"indicator":"指标名","note":"一句话"}]}
"""


def run_followup_notes(api_key, items, model=None, llm_base=None):
    if not items:
        return {"notes": []}

    user_content = json.dumps(items, ensure_ascii=False)
    msg, usage = chat(api_key, [
        {"role": "system", "content": SYSTEM_NOTE},
        {"role": "user", "content": user_content},
    ], model=model, base_url=llm_base)

    parsed = parse_json_block(msg.get("content")) or {}
    notes = []
    for item in parsed.get("notes") or []:
        text = item.get("note") or ""
        if not text or any(w in text for w in FORBIDDEN):
            continue
        notes.append({"indicator": item.get("indicator"), "note": text})

    return {"notes": notes, "usage": usage}
