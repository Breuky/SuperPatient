# SuperPatient API 文档

> **Base URL**：`http://localhost:8080`
> **认证**：除登录、注册外，所有接口都需要请求头 `token: <JWT>`
> **请求/响应格式**：`application/json`（上传接口为 `multipart/form-data`）

## 统一响应

```json
{ "code": 1, "msg": "success", "data": {} }
```

| code | 含义 |
|------|------|
| 1 | 成功 |
| 0 | 业务失败，`msg` 可直接展示给用户 |

分页响应的 `data` 为 `{ "total": 50, "rows": [...] }`。

鉴权失败返回 HTTP 401（无响应体）；访问他人数据返回 `code=0` 且提示「无权访问」。

---

## 一、认证

### 1.1 登录

`POST /api/auth/login`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 明文密码，服务端 BCrypt 校验 |

```json
{ "username": "demo", "password": "demo1234" }
```

> 用户名不存在与密码错误统一返回 `用户名或密码错误`，不暴露账号是否存在。

### 1.2 注册

`POST /api/auth/register`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 3-32 位，字母/数字/下划线/中文 |
| password | String | 是 | 至少 6 位 |
| nickname | String | 否 | 不填则与用户名相同 |
| gender | Integer | 是 | 0=女 1=男，用于匹配参考区间 |

注册成功直接返回 Token：

```json
{
  "code": 1,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expireIn": 43200,
    "userInfo": {
      "id": 1, "username": "demo", "nickname": "示例用户",
      "gender": 1, "genderText": "男",
      "birthDate": "1985-06-12", "age": 41,
      "avatarUrl": null, "phone": null, "notes": null
    }
  },
  "msg": "success"
}
```

### 1.3 获取个人信息

`GET /api/auth/profile`

返回结构同上面的 `userInfo`。`gender` / `birthDate` 用于参考区间匹配，未填时该用户只能使用指标默认区间。

### 1.4 更新个人信息

`PUT /api/auth/profile`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| nickname | String | 否 | 昵称 |
| avatarUrl | String | 否 | 头像地址 |
| phone | String | 否 | 手机号 |
| gender | Integer | 否 | 0=女 1=男 |
| birthDate | String | 否 | `yyyy-MM-dd` |
| notes | String | 否 | 既往病史、过敏史等 |

> 全量覆盖：未传的字段会被置空，前端应始终提交完整表单。

### 1.5 登出

`POST /api/auth/logout`

JWT 无状态，服务端不维护会话，此接口仅用于前端统一调用；实际登出是删除本地 Token。

### 1.6 统计信息

`GET /api/auth/statistics`

```json
{ "code": 1, "data": { "recordCount": 12, "indicatorCount": 9 }, "msg": "success" }
```

---

## 二、检查分类

### 2.1 分类列表（含指标）

`GET /api/categories`

```json
{
  "code": 1,
  "data": [
    {
      "id": 1, "name": "抽血", "icon": "blood", "sortOrder": 1,
      "indicators": [
        { "id": 1, "name": "血红蛋白", "unit": "g/L", "valueType": "number",
          "normalMin": 120, "normalMax": 160 }
      ]
    }
  ],
  "msg": "success"
}
```

### 2.2 新增 / 更新 / 删除

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/categories` | 新增分类（`name`、`icon`、`sortOrder`） |
| PUT | `/api/categories/{id}` | 更新分类 |
| DELETE | `/api/categories/{id}` | 删除分类（级联删除其下指标） |

---

## 三、指标

### 3.1 指标列表

`GET /api/indicators?categoryId={id}`

`categoryId` 可选，不传返回全部启用指标。**只返回当前用户可见的指标**：公共指标（`ownerId = 0`）加上自己的私有指标。

返回每项的 `id / categoryId / ownerId / name / aliases / unit / valueType / normalMin / normalMax / sortOrder / status`。

> `ownerId = 0` 表示系统公共指标，其他值表示该指标只对创建者可见。别名（`aliases`）用于抽取归一，逗号分隔，可在指标表单里直接编辑。
>
> 维护类接口会校验归属：别人的私有指标一律返回「指标不存在或无权修改」。

### 3.2 新增 / 更新 / 启停 / 删除

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/indicators` | 新增指标，**归属当前登录用户**（私有） |
| PUT | `/api/indicators/{id}` | 更新指标，仅限自己的 |
| PUT | `/api/indicators/{id}/status` | 启用/禁用，body：`{ "status": 0 }`，仅限自己的 |
| DELETE | `/api/indicators/{id}` | 物理删除，仅限自己的且无历史数据 |

公共指标（`ownerId = 0`）由系统维护，以上四个接口对它的写入一律返回「指标不存在或无权修改」。
同分类下不允许重名：新建或改名时与公共库或自己的指标撞名，返回「该分类下已有同名指标」。

---

## 四、参考区间规则

同一指标在不同性别/年龄段区间不同。判定优先级：

```
报告照片上印的区间（exam_value.ref_*）
        ↓ 没有
性别/年龄规则命中（reference_range）
        ↓ 没有
指标默认值（exam_indicator.normal_*）
```

### 4.1 查询某指标的规则

`GET /api/indicators/{indicatorId}/reference-ranges`

```json
{
  "code": 1,
  "data": [
    { "id": 1, "indicatorId": 1, "gender": 1, "ageMin": 18, "ageMax": null,
      "refMin": 130, "refMax": 175, "refText": "130-175",
      "source": "成人男性参考区间", "priority": 10 },
    { "id": 3, "indicatorId": 1, "gender": null, "ageMin": 0, "ageMax": 17,
      "refMin": 110, "refMax": 160, "refText": "110-160",
      "source": "儿童参考区间", "priority": 10 }
  ],
  "msg": "success"
}
```

### 4.2 新增 / 更新 / 删除

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/reference-ranges` | 新增规则 |
| PUT | `/api/reference-ranges/{id}` | 全量更新 |
| DELETE | `/api/reference-ranges/{id}` | 删除规则 |

请求体：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| indicatorId | Integer | 是 | 指标 ID（仅新增时） |
| gender | Integer | 否 | null=不限，0=女，1=男 |
| ageMin / ageMax | Integer | 否 | 年龄区间，含端点，null 表示不限 |
| refMin / refMax | BigDecimal | 否 | 至少填一个 |
| refText | String | 否 | 区间原文 |
| source | String | 否 | 来源说明 |
| priority | Integer | 否 | 越大越优先，默认 0 |

**匹配规则**：性别与年龄需同时命中；命中项先比 `priority`，再比具体程度（性别+年龄 &gt; 仅性别 &gt; 仅年龄），最后取年龄区间更窄的一条。用户未填出生日期时，限定年龄的规则会被跳过。

---

## 四之二、关注指标

用户把自己长期关心的指标标星，录入页默认展开、趋势页可一键过滤。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/focus-indicators` | 我关注的指标（返回指标详情数组） |
| POST | `/api/focus-indicators/{indicatorId}` | 关注（幂等，重复调用不报错） |
| DELETE | `/api/focus-indicators/{indicatorId}` | 取消关注 |

---

## 五、检查记录

记录直接挂在用户上，无需传 `userId`，服务端从 Token 取。

### 5.1 我的检查记录列表

`GET /api/records`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryId | Integer | 否 | 按分类筛选 |
| startDate | String | 否 | `yyyy-MM-dd` |
| endDate | String | 否 | `yyyy-MM-dd` |
| page | Integer | 否 | 默认 1 |
| pageSize | Integer | 否 | 默认 20 |

```json
{
  "code": 1,
  "data": {
    "total": 4,
    "rows": [
      { "id": 10, "categoryId": 1, "categoryName": "抽血",
        "examDate": "2026-08-01T09:00:00", "hospital": "市第一人民医院",
        "doctor": "王医生", "imageUrl": "/api/files/3",
        "totalIndicators": 2, "abnormalCount": 1 }
    ]
  },
  "msg": "success"
}
```

### 5.2 检查记录详情

`GET /api/records/{id}`

```json
{
  "code": 1,
  "data": {
    "id": 10, "userId": 1, "categoryId": 1, "categoryName": "抽血",
    "examDate": "2026-08-01T09:00", "hospital": "市第一人民医院",
    "doctor": "王医生", "notes": null, "imageUrl": "/api/files/3",
    "values": [
      {
        "id": 41, "indicatorId": 1, "indicatorName": "血红蛋白",
        "unit": "g/L", "valueType": "number",
        "valueDecimal": 128.0, "valueText": null,
        "normalMin": 130.0, "normalMax": 175.0,
        "refMin": null, "refMax": null, "refText": null,
        "refSource": "rule", "rawName": null, "rawUnit": null,
        "isAbnormal": 2, "abnormalText": "偏低"
      }
    ]
  },
  "msg": "success"
}
```

字段说明：

| 字段 | 说明 |
|------|------|
| normalMin / normalMax | **生效**的参考区间 |
| refMin / refMax / refText | 本次报告的快照区间，为空表示该值没存快照 |
| refSource | `report`=报告印刷 / `rule`=规则推算 / `default`=指标默认 |
| rawName / rawUnit | 抽取得到的报告原文，用于回溯 |
| isAbnormal | 0=正常 1=偏高 2=偏低，写入时按生效区间算好 |

### 5.3 新增检查记录

`POST /api/records`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryId | Integer | 是 | 检查分类 |
| examDate | String | 是 | `yyyy-MM-ddTHH:mm:ss` |
| hospital | String | 否 | 检查医院 |
| doctor | String | 否 | 开单/主治医生 |
| notes | String | 否 | 备注 |
| imageUrl | String | 否 | 上传接口返回的地址 |
| values | Array | 是 | 指标值列表 |

`values` 每项：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| indicatorId | Integer | 是 | 指标 ID |
| valueDecimal | BigDecimal | 否 | 数值型 |
| valueText | String | 否 | 文本型 |
| refMin / refMax | BigDecimal | 否 | 本次报告的参考区间 |
| refText | String | 否 | 区间原文 |
| rawName / rawUnit | String | 否 | 报告原文（抽取溯源） |

```json
{
  "categoryId": 1,
  "examDate": "2026-08-01T09:00:00",
  "hospital": "市第一人民医院",
  "values": [
    { "indicatorId": 1, "valueDecimal": 128 },
    { "indicatorId": 14, "valueDecimal": 300 }
  ]
}
```

响应返回 `id`、`abnormalCount` 与每项判定结果。

> 传入 `refMin/refMax` 时按报告区间判定并落快照；不传则依次尝试规则表、指标默认值。

### 5.4 更新检查记录

`PUT /api/records/{id}`

请求体同新增，但 `categoryId` 不可改；`values` 传了就**全量覆盖**（先删后插），不传则只更新记录本身。

### 5.5 删除检查记录

`DELETE /api/records/{id}`

级联删除该记录下所有指标值。

---

## 六、趋势

### 6.1 指标趋势

`GET /api/trends?indicatorId={id}`

可选 `startDate` / `endDate`。

```json
{
  "code": 1,
  "data": {
    "indicator": { "id": 2, "name": "白细胞计数", "unit": "10^9/L",
                   "normalMin": 4.0, "normalMax": 10.0 },
    "points": [
      { "recordId": 4, "examDate": "2025-09-15", "value": 6.8,
        "isAbnormal": 0, "refMin": 4.0, "refMax": 10.0, "refText": "4.0-10.0" },
      { "recordId": 5, "examDate": "2025-10-20", "value": 5.4,
        "isAbnormal": 0, "refMin": 3.5, "refMax": 9.5, "refText": "3.5-9.5" }
    ]
  },
  "msg": "success"
}
```

> `points[].refMin/refMax` 是该点自己的参考区间；前端画参考带时按点取值，才能体现"不同医院区间不同"。

### 6.2 可查趋势的指标

`GET /api/trendable-indicators`

返回当前用户有数值记录的指标，含 `indicatorId / indicatorName / unit / categoryId / recordCount`。

---

## 七、文件

### 7.1 上传报告照片

`POST /api/files/upload`（`multipart/form-data`）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | jpg / png，最大 10MB |
| type | String | 否 | `report`（默认）/ `avatar` |

```json
{ "code": 1, "data": { "url": "/api/files/12", "fileName": "report_20260801_a1b2c3d4.png", "fileSize": 1024000 }, "msg": "success" }
```

### 7.2 读取报告照片

`GET /api/files/{id}`

- 仅上传者本人可读
- 鉴权支持 `token` 请求头或 `?token=xxx`（`<img>` 无法带请求头）
- 非本人或不存在返回 **404**，不泄露文件是否存在

---

## 八、化验单抽取

`POST /api/parse/report`

把图片交给多模态模型抽取指标，**只返回候选值，不落库**。前端在校对页确认后再调 `POST /api/records`。

```json
{ "imageUrl": "/api/files/12", "categoryId": 1, "doubleRead": false }
```

`doubleRead` 为 `true` 时走**精读**：同一张图识别两次，逐行比对，只对两次不一致的行再调一次模型仲裁。耗时与成本约 2-3 倍，默认关闭（可用环境变量 `DOUBLE_READ=true` 改默认值）。

冲突行在候选里多一个 `conflict` 字段：

```json
"conflict": {
  "first": 34, "second": 340, "resolved": 340,
  "reason": "参考区间 316-354，34 少一位",
  "agreement": false, "missingInOneRead": false
}
```

`resolved` 为空表示仲裁也看不清，此时保留第一次读到的值，两个数一起交给用户判断。

```json
{
  "code": 1,
  "data": {
    "categoryId": 1,
    "examDate": "2026-03-28",
    "hospital": "市第一人民医院",
    "engine": "mock",
    "candidates": [
      {
        "rawName": "WBC", "indicatorId": 2, "indicatorName": "白细胞计数",
        "unit": "10^9/L", "rawUnit": "10^9/L",
        "valueDecimal": 3.2, "valueText": null,
        "refMin": 4.0, "refMax": 10.0, "refText": "4.0-10.0",
        "isAbnormal": 2, "matchType": "alias", "confidence": 0.95, "warning": null
      }
    ],
    "unmatched": [
      { "rawName": "嗜酸性粒细胞百分比", "rawValue": "2.1", "rawUnit": "%", "refText": "0.4-8.0" }
    ]
  },
  "msg": "success"
}
```

| 字段 | 说明 |
|------|------|
| matchType | `exact` 规范名精确 / `alias` 别名 / `contains` 包含匹配 |
| confidence | 匹配置信度，前端可弱化低置信项 |
| unmatched | 指标库里没有的项，需用户手动指定或丢弃 |
| warning | 单位换算、数值超出合理范围等提示 |

模型由 `app.llm.provider` 控制：`mock`（默认，返回固定样本）/ `qwen`（需 `DASHSCOPE_API_KEY`）。

---

## 九、随访提醒

基于**用户自己的历史复查间隔**推算的待办，不包含医疗建议，也不调用模型。

规则（全部确定性）：

1. 最近一次结果异常就生成提醒——**不必等到期**，到期与否只影响界面上的紧迫度
2. 至少 2 次记录才能算出「既往间隔中位数」
3. 连续 2 次异常 → 间隔减半，标记高优先级
4. 指标有了更新的检查、或最新结果恢复正常 → 旧待办自动关闭

`overdueDays > 0` 表示已超过建议日期；`< 0` 表示还剩多少天（界面显示「还有 N 天」）。

### 9.1 待复查列表

`GET /api/follow-ups`

读取时会重算一次（幂等），返回 `pending` 项，按优先级、到期日排序。

```json
{
  "code": 1,
  "data": [
    {
      "id": 12,
      "indicatorId": 2,
      "indicatorName": "白细胞计数",
      "unit": "10^9/L",
      "lastExamDate": "2026-06-01",
      "intervalDays": 33,
      "dueDate": "2026-07-04",
      "overdueDays": 113,
      "priority": 0,
      "reason": "近 3 次里有 1 次偏高；你既往的复查间隔中位是 33 天",
      "status": "pending"
    }
  ]
}
```

`overdueDays` 为负数表示还没到期。

### 9.2 标记完成 / 忽略

`PUT /api/follow-ups/{id}`

```json
{ "status": "dismissed" }
```

`done` = 已经复查过；`dismissed` = 这次不想看到。被忽略的提醒不会复活，但下一次检查后仍会按新数据重新计算。

---

## 十、报告解读（Agent）

记录详情页的对话接口。模型拿到 `recordId` 和问题后自己决定调哪些只读工具（`find_indicator` / `list_records` / `get_record` / `get_trend` / `get_reference_rules`），Java 负责鉴权、证据包、免责声明与降级。

### 10.1 提问

`POST /api/insights/report`

```json
{
  "recordId": 1,
  "question": "那最近一次检查是什么时候？",
  "mode": "agent",
  "history": [
    { "role": "user", "content": "这次有哪几项超出参考区间？" },
    { "role": "assistant", "content": "本次检查中，空腹血糖和总胆固醇两项超出参考区间。" }
  ]
}
```

| 字段 | 说明 |
|---|---|
| `mode` | `agent`（默认，工具调用循环）/ `simple`（证据包单次生成，仅兜底） |
| `history` | 之前的问答，最多取最近 8 轮、每轮 600 字；不传即为新一轮对话 |

响应：

```json
{
  "code": 1,
  "data": {
    "mode": "agent",
    "summary": "最近一次检查是2026年06月15日，在市第一人民医院做的CT检查。",
    "highlights": [{ "indicator": "CT", "text": "…", "basis": "list_records" }],
    "warnings": [],
    "offTopic": false,
    "suggestions": [],
    "outOfScope": false,
    "scopeNote": "",
    "turns": 2,
    "trace": [{ "tool": "list_records", "args": { "limit": 10 }, "ms": 14, "ok": true }],
    "usage": { "total_tokens": 2103 },
    "degraded": false,
    "disclaimer": "以上内容仅是本系统对历史数据的描述，不构成医疗建议；如有疑问请咨询医生。"
  }
}
```

`offTopic=true` 表示问题与检查数据无关（天气、写诗、编程等），此时不调用模型、不返回数值，`suggestions` 给出可点的示例问题。

`outOfScope=true` 表示用户问的是病因、危害、要不要治疗这类医学判断，此时只返回与问题相关的事实数据，`scopeNote` 说明系统能提供什么。

`degraded=true` 表示已降级为规则摘要，`mode` 会是 `rule`，`warnings[0]` 给出原因。`disclaimer` 由服务端拼接，不接受模型生成。

### 10.2 调用留痕

`GET /api/insights/logs?limit=50`

返回本人最近的调用：`mode / question / category / turns / toolCalls / tokens / elapsedMs / degraded / summary / traceJson`。

### 10.3 按题型统计

`GET /api/insights/stats`

按题型（异常概览、趋势对比、跨记录、单指标取值、总体概括、越界提问）聚合条数、平均轮数、平均工具次数、平均 token、平均耗时与降级次数。

---

## 限流

超出限额时返回 **HTTP 429**，并带 `Retry-After`（秒）：

```json
{ "code": 0, "msg": "操作过于频繁，请 60 秒后再试" }
```

| 接口 | 维度 | 限额 |
|---|---|---|
| POST /auth/login | IP | 10 次 / 分钟 |
| POST /auth/login | 账号 | 5 次 / 10 分钟（成功清零） |
| POST /auth/register | IP | 3 次 / 小时 |
| POST /parse/report | 用户 | 20 次 / 天、3 次 / 分钟 |
| POST /insights/report | 用户 | 100 次 / 天、10 次 / 分钟 |
| POST /files/upload | 用户 | 30 次 / 天 |

## 接口汇总

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录 | 否 |
| POST | `/api/auth/register` | 注册 | 否 |
| GET | `/api/auth/profile` | 个人信息 | 是 |
| PUT | `/api/auth/profile` | 更新个人信息 | 是 |
| POST | `/api/auth/logout` | 登出 | 是 |
| GET | `/api/auth/statistics` | 统计信息 | 是 |
| GET | `/api/categories` | 分类列表 | 是 |
| POST | `/api/categories` | 新增分类 | 是 |
| PUT | `/api/categories/{id}` | 更新分类 | 是 |
| DELETE | `/api/categories/{id}` | 删除分类 | 是 |
| GET | `/api/indicators` | 指标列表 | 是 |
| POST | `/api/indicators` | 新增指标 | 是 |
| PUT | `/api/indicators/{id}` | 更新指标 | 是 |
| PUT | `/api/indicators/{id}/status` | 启用/禁用指标 | 是 |
| DELETE | `/api/indicators/{id}` | 删除指标 | 是 |
| GET | `/api/indicators/{id}/reference-ranges` | 参考区间规则 | 是 |
| POST | `/api/reference-ranges` | 新增规则 | 是 |
| PUT | `/api/reference-ranges/{id}` | 更新规则 | 是 |
| DELETE | `/api/reference-ranges/{id}` | 删除规则 | 是 |
| GET | `/api/focus-indicators` | 我关注的指标 | 是 |
| POST | `/api/focus-indicators/{indicatorId}` | 关注指标 | 是 |
| DELETE | `/api/focus-indicators/{indicatorId}` | 取消关注 | 是 |
| GET | `/api/records` | 记录列表 | 是 |
| GET | `/api/records/{id}` | 记录详情 | 是 |
| POST | `/api/records` | 新增记录 | 是 |
| PUT | `/api/records/{id}` | 更新记录 | 是 |
| DELETE | `/api/records/{id}` | 删除记录 | 是 |
| GET | `/api/trends` | 指标趋势 | 是 |
| GET | `/api/trendable-indicators` | 可查趋势指标 | 是 |
| POST | `/api/files/upload` | 上传图片 | 是 |
| GET | `/api/files/{id}` | 读取图片（仅上传者） | 是 |
| POST | `/api/parse/report` | 化验单抽取 | 是 |
