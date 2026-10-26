# Agent 服务

报告解读的 Python 侧，**只用标准库**，不需要 pip 安装任何依赖。

## 为什么单独一个进程

| 层 | 位置 | 性质 |
|---|---|---|
| 取数、鉴权、落库、免责声明、降级 | Spring Boot | 确定性、有副作用 |
| prompt 编排、工具调用决策 | 本服务 | 概率性、无状态、无副作用 |

本服务**不连数据库**，所有数据通过 Java 的只读接口取，鉴权沿用用户自己的 JWT（由 Java 透传）。

## 换模型端点

默认 DashScope（通义）。换成任意 OpenAI 兼容端点（DeepSeek / 智谱 / 本地 vLLM）只需三个环境变量，代码不动：

```bash
export AGENT_MODEL=deepseek-chat
export AGENT_LLM_BASE_URL=https://api.deepseek.com
export AGENT_API_KEY=sk-...
python agent/server.py --port 8001
```

端点优先级：请求头 `X-Llm-Base`（Java 透传）> `AGENT_LLM_BASE_URL` > `LLM_BASE_URL` > 内置 DashScope。
个别端点不支持 `tool_choice=required`，代码会自动退回 `auto` 重试一次。

注意：`deepseek-reasoner` 这类推理模型不支持 function calling，只能用 `deepseek-chat`。

## 启动

```bash
python agent/server.py --port 8001
# 后端启动时打开开关
AGENT_ENABLED=true mvn spring-boot:run
```

## 只允许一个实例

Windows 下 `HTTPServer.allow_reuse_address=True` 会让**两个进程同时绑定 8001**，请求随机落到旧实例，表现为"改了配置不生效"或"莫名其妙的 401"。本服务已改成 `allow_reuse_address = False`：端口被占时直接报错退出。

## 两种模式

| 端点 | 模式 | 说明 |
|---|---|---|
| `POST /agent/summarize` | simple | Java 已经组装好证据包，这里只做一次生成，0 次工具调用 |
| `POST /agent/report-insight` | agent | 只拿到 `recordId` + 问题，模型自己决定查什么 |

工具选择只发生在 `tools` 参数这一层：能力由 schema 声明，"什么时候用"写在各自的 `description` 里。系统提示词只写目标、硬约束和输出格式，**不写"哪类问题用哪个工具"**——那样等于把规划写死。是否必须先取数由代码强制（首轮 `tool_choice=required`），不靠提示词提醒。

## 可用的只读工具

| 工具 | 用途 |
|---|---|
| `find_indicator(keyword)` | 「血脂」→ 具体指标 id；同时查可选指标与「有历史但已停用」的指标 |
| `overview()` | 全部记录的宏观统计：次数、时间范围、分类计数、异常 Top5 |
| `get_record(record_id)` | 本次各项值、单位、参考区间、异常标记 |
| `get_trend(indicator_id, limit)` | 该指标历史序列 |
| `get_reference_rules(indicator_id)` | 性别/年龄区间规则 |

## 护栏

| 机制 | 说明 |
|---|---|
| 最大轮数 6 / 超时 20s | 超了强制收尾 |
| 重复调用拦截 | 同样的工具+参数第二次直接回 error，逼模型换策略 |
| 首轮强制取数 | trace 为空时 `tool_choice=required`，拿到数据后回到 `auto`；不依赖模型"愿不愿意查" |
| 未调用工具即作答 | 连续两次拽回仍不调工具，判定为编数据，返回失败让 Java 降级为规则摘要 |
| 无关问题 | agent/scope.py 关键词前置拦截，不调模型、不查数据，直接返回「只能回答检查相关问题」+ 示例 |
| 越界提问 | 病因、危害、要不要治疗这类问题：摘要声明「属于医学判断」并置 `out_of_scope`，只给相关数据；前端用蓝色提示条区分 |
| 医疗建议过滤 | 命中「建议/治疗/用药/剂量/确诊/患有…」的句子直接丢弃；摘要命中则整段换成范围说明 |
| 免责声明 | 由 Java 拼接，模型无权生成 |

## 实测轨迹差异

```
问「这次有哪些指标超出参考区间」 → get_record                     2 轮 3.3s
问「我的血糖最近怎么样」         → find_indicator → get_record
                                    → get_trend(id=6)            4 轮 6.3s
问「看看甘油三酯」               → get_record（发现未检测，直接说明） 2 轮 2.1s
```

同一套代码，路径和轮数由模型自己决定——这是它和「一次 API 调用」的区别。
