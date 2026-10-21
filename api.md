# SuperPatient API 接口文档

> **Base URL**: `http://localhost:8080`
> **认证方式**: JWT Token（请求头 `token`）
> **请求格式**: `application/json`
> **响应格式**: `application/json`

---

## 统一响应格式

### 成功响应

```json
{
  "code": 1,
  "msg": "success",
  "data": { ... }
}
```

### 分页响应

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "total": 50,
    "rows": [ ... ]
  }
}
```

### 错误响应

```json
{
  "code": 0,
  "msg": "参数错误: patientId 不能为空",
  "data": null
}
```

## 业务错误码

| code | 说明 |
|------|------|
| 1 | 成功 |
| 0 | 业务错误（msg 中附带具体原因） |

> HTTP 状态码层面：成功返回 200，认证失败返回 401，服务器异常返回 500。
> `code` 字段仅表示业务层面是否成功，不重复 HTTP 状态码的职责。

---

## 一、认证模块

> Web 端采用「用户名 + 密码」登录，服务端签发 JWT。除登录、注册外，所有接口都需要在请求头带 `token`。

### 1.1 登录

`POST /api/auth/login`

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 明文密码，服务端用 BCrypt 校验 |

**请求示例**

```json
{
  "username": "demo",
  "password": "demo1234"
}
```

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expireIn": 43200,
    "userInfo": {
      "id": 1,
      "nickname": "示例用户",
      "avatarUrl": null,
      "phone": null
    }
  }
}
```

**失败**：用户名不存在与密码错误统一返回 `用户名或密码错误`，不暴露账号是否存在。

### 1.2 注册

`POST /api/auth/register`

注册成功即返回 Token，无需再调一次登录。系统会自动为账号创建一条 1:1 的患者档案。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 3-32 位，字母/数字/下划线/中文 |
| password | String | 是 | 至少 6 位，服务端 BCrypt 存储 |
| nickname | String | 否 | 不填则与用户名相同 |
| gender | Integer | 是 | 0=女 1=男，用于匹配参考区间 |

**响应**：与登录一致（`token` / `expireIn` / `userInfo`）。

### 1.3 获取用户信息

`GET /api/auth/profile`

**Header**

```
token: eyJhbGciOiJIUzI1NiJ9...
```

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "nickname": "测试医生",
    "avatarUrl": null,
    "phone": "13800138000"
  }
}
```

### 1.4 更新用户信息

`PUT /api/auth/profile`

**Header**

```
token: eyJhbGciOiJIUzI1NiJ9...
```

**请求参数**（所有字段可选）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| nickname | String | 否 | 昵称 |
| avatarUrl | String | 否 | 头像 URL |
| phone | String | 否 | 手机号 |

**请求示例**

```json
{
  "nickname": "张医生",
  "avatarUrl": "https://example.com/avatar.png"
}
```

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "nickname": "张医生",
    "avatarUrl": "https://example.com/avatar.png",
    "phone": "13800138000"
  }
}
```

### 1.5 登出

`POST /api/auth/logout`

**Header**

```
token: eyJhbGciOiJIUzI1NiJ9...
```

> 客户端移除本地 token 即可。此接口为预留，可用于服务端 Token 黑名单等扩展。

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": null
}
```

### 1.6 获取用户统计信息

`GET /api/auth/statistics`

> 返回当前用户的统计数据，供"我的"页面展示。
> - `patientCount`: 当前用户创建的患者数
> - `recordCount`: 当前用户录入的检查记录总数
> - `indicatorCount`: 当前用户所有患者曾使用过的启用状态指标的种类数（从 `exam_value` 去重计数，非系统全局模板数）

**Header**

```
token: eyJhbGciOiJIUzI1NiJ9...
```

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "patientCount": 3,
    "recordCount": 24,
    "indicatorCount": 15
  }
}
```

---

## 二、患者管理模块

### 2.1 患者列表（分页）

`GET /api/patients`

> 仅返回当前登录用户创建的患者，根据 JWT Token 自动过滤 `user_id`。

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词（姓名） |
| page | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 10 |

> `abnormalCount` 为该患者所有检查记录中异常指标的去重计数（同一指标多次异常只算一次）。

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "total": 1,
    "rows": [
      {
        "id": 1,
        "name": "张明",
        "gender": 1,
        "genderText": "男",
        "age": 16,
        "diagnosisDate": "2025-03-15",
        "lastExamDate": "2026-07-28",
        "totalExams": 12,
        "abnormalCount": 3
      }
    ]
  }
}
```

### 2.2 患者详情

`GET /api/patients/{id}`

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 患者ID |

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "name": "张明",
    "gender": 1,
    "genderText": "男",
    "birthDate": "2010-05-12",
    "age": 16,
    "diagnosisDate": "2025-03-15",
    "phone": "13800138000",
    "notes": "年度体检，无特殊病史",
    "stats": {
      "totalExams": 12,
      "lastExamDate": "2026-07-28",
      "categoryStats": [
        { "categoryName": "抽血", "count": 8, "lastDate": "2026-07-28" },
        { "categoryName": "CT",   "count": 3, "lastDate": "2026-06-15" },
        { "categoryName": "MRI",  "count": 1, "lastDate": "2026-04-10" }
      ]
    }
  }
}
```

### 2.3 新增患者

`POST /api/patients`

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 患者姓名 |
| gender | Integer | 是 | 性别: 0=女 1=男 |
| birthDate | String | 否 | 出生日期 (yyyy-MM-dd) |
| diagnosisDate | String | 否 | 确诊日期 (yyyy-MM-dd) |
| phone | String | 否 | 联系电话 |
| notes | String | 否 | 备注 |

**请求示例**

```json
{
  "name": "李芳",
  "gender": 0,
  "birthDate": "2012-08-20",
  "diagnosisDate": "2025-06-01",
  "phone": "13900139000",
  "notes": "慢病随访，无特殊病史"
}
```

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 2,
    "name": "李芳",
    "gender": 0,
    "birthDate": "2012-08-20",
    "diagnosisDate": "2025-06-01",
    "phone": "13900139000",
    "notes": "慢病随访，无特殊病史"
  }
}
```

### 2.4 更新患者

`PUT /api/patients/{id}`

**请求参数**: 同 2.3，所有字段均为可选。

### 2.5 删除患者

`DELETE /api/patients/{id}`

> 删除患者会级联删除该患者的所有检查记录和指标值。

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": null
}
```

---

## 三、检查分类模块

### 3.1 分类列表（含指标）

`GET /api/categories`

> 仅返回启用状态（`status=1`）的指标，已禁用的指标不会出现。

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "name": "抽血",
      "icon": "blood",
      "sortOrder": 1,
      "indicators": [
        {
          "id": 1,
          "name": "血红蛋白",
          "unit": "g/L",
          "valueType": "number",
          "normalMin": 120.0,
          "normalMax": 160.0
        },
        {
          "id": 2,
          "name": "白细胞计数",
          "unit": "10^9/L",
          "valueType": "number",
          "normalMin": 4.0,
          "normalMax": 10.0
        }
      ]
    },
    {
      "id": 2,
      "name": "CT",
      "icon": "ct",
      "sortOrder": 2,
      "indicators": [
        {
          "id": 20,
          "name": "检查部位",
          "unit": null,
          "valueType": "text",
          "normalMin": null,
          "normalMax": null
        },
        {
          "id": 21,
          "name": "影像描述",
          "unit": null,
          "valueType": "text",
          "normalMin": null,
          "normalMax": null
        }
      ]
    }
  ]
}
```

### 3.2 新增分类

`POST /api/categories`

**请求示例**

```json
{
  "name": "骨扫描",
  "icon": "bone-scan",
  "sortOrder": 6
}
```

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 6,
    "name": "骨扫描",
    "icon": "bone-scan",
    "sortOrder": 6
  }
}
```

### 3.3 更新分类

`PUT /api/categories/{id}`

**请求参数**（所有字段可选）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 否 | 分类名称 |
| icon | String | 否 | 图标标识 |
| sortOrder | Integer | 否 | 排序序号 |

**请求示例**

```json
{
  "name": "全身骨扫描",
  "sortOrder": 5
}
```

### 3.4 删除分类

`DELETE /api/categories/{id}`

> 删除分类会级联删除该分类下的所有指标和检查值。已有历史数据的分类建议保留，避免数据丢失。

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": null
}
```

---

## 四、检查记录模块

### 4.1 患者的检查记录列表

`GET /api/patients/{patientId}/records`

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryId | Integer | 否 | 按分类筛选 |
| startDate | String | 否 | 起始日期 (yyyy-MM-dd) |
| endDate | String | 否 | 截止日期 (yyyy-MM-dd) |
| page | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 20 |

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "total": 3,
    "rows": [
      {
        "id": 2,
        "categoryId": 1,
        "categoryName": "抽血",
        "examDate": "2026-05-20T10:00:00",
        "hospital": "市第一人民医院",
        "doctor": "陈主任",
        "abnormalCount": 3,
        "totalIndicators": 4
      },
      {
        "id": 3,
        "categoryId": 2,
        "categoryName": "CT",
        "examDate": "2026-06-15T14:00:00",
        "hospital": "市第一人民医院",
        "doctor": "陈主任",
        "abnormalCount": 0,
        "totalIndicators": 6,
        "imageUrl": "https://oss.example.com/report_003.jpg"
      }
    ]
  }
}
```

### 4.2 检查记录详情

`GET /api/records/{id}`

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 2,
    "patientId": 1,
    "patientName": "张明",
    "categoryId": 1,
    "categoryName": "抽血",
    "examDate": "2026-05-20T10:00:00",
    "hospital": "市第一人民医院",
    "doctor": "陈主任",
    "notes": "复查",
    "imageUrl": null,
    "values": [
      {
        "id": 5,
        "indicatorId": 1,
        "indicatorName": "血红蛋白",
        "unit": "g/L",
        "valueType": "number",
        "valueDecimal": 125.0,
        "valueText": null,
        "isAbnormal": 0,
        "abnormalText": "正常",
        "normalMin": 120.0,
        "normalMax": 160.0
      },
      {
        "id": 6,
        "indicatorId": 2,
        "indicatorName": "白细胞计数",
        "unit": "10^9/L",
        "valueType": "number",
        "valueDecimal": 3.2,
        "valueText": null,
        "isAbnormal": 2,
        "abnormalText": "偏低",
        "normalMin": 4.0,
        "normalMax": 10.0
      },
      {
        "id": 8,
        "indicatorId": 9,
        "indicatorName": "C反应蛋白",
        "unit": "mg/L",
        "valueType": "number",
        "valueDecimal": 18.5,
        "valueText": null,
        "isAbnormal": 1,
        "abnormalText": "偏高",
        "normalMin": 0.0,
        "normalMax": 10.0
      }
    ]
  }
}
```

### 4.3 新增检查记录

`POST /api/records`

> 一次请求同时创建记录和所有指标值。后端自动计算 `isAbnormal`。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| patientId | Long | 是 | 患者ID |
| categoryId | Integer | 是 | 检查分类ID |
| examDate | String | 是 | 检查日期时间 (yyyy-MM-ddTHH:mm:ss) |
| hospital | String | 否 | 检查医院 |
| doctor | String | 否 | 主治医生 |
| notes | String | 否 | 备注 |
| imageUrl | String | 否 | 报告照片URL（先调上传接口获取） |
| values | Array | 是 | 指标值列表 |

**values 数组中每项的字段**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| indicatorId | Integer | 是 | 指标ID |
| valueDecimal | BigDecimal | 否 | 数值型测量值（数值型指标使用） |
| valueText | String | 否 | 文本型测量值（文本型指标使用，如CT描述等） |

> `valueDecimal` 和 `valueText` 二选一传入。数值型指标传 `valueDecimal`，文本型指标传 `valueText`。

**请求示例（数值型指标）**

```json
{
  "patientId": 1,
  "categoryId": 1,
  "examDate": "2026-08-01T09:30:00",
  "hospital": "市第一人民医院",
  "doctor": "陈主任",
  "notes": "复查前检查",
  "imageUrl": "https://oss.example.com/report_004.jpg",
  "values": [
    { "indicatorId": 1, "valueDecimal": 105.0 },
    { "indicatorId": 2, "valueDecimal": 8.2 },
    { "indicatorId": 5, "valueDecimal": 180.0 },
    { "indicatorId": 9, "valueDecimal": 15.3 }
  ]
}
```

**请求示例（含文本型指标，如 CT/MRI）**

```json
{
  "patientId": 1,
  "categoryId": 2,
  "examDate": "2026-08-01T14:00:00",
  "hospital": "市第一人民医院",
  "doctor": "陈主任",
  "notes": "复查影像评估",
  "imageUrl": "https://oss.example.com/ct_20260801.jpg",
  "values": [
    { "indicatorId": 11, "valueDecimal": 38.0 },
    { "indicatorId": 12, "valueText": "右股骨远端干骺端" },
    { "indicatorId": 13, "valueText": "较前片未见明显变化" },
    { "indicatorId": 16, "valueText": "较前片未见明显变化" }
  ]
}
```

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 4,
    "patientId": 1,
    "categoryId": 1,
    "examDate": "2026-08-01T09:30:00",
    "abnormalCount": 2,
    "values": [
      {
        "indicatorId": 1,
        "indicatorName": "血红蛋白",
        "valueDecimal": 105.0,
        "valueText": null,
        "isAbnormal": 2,
        "abnormalText": "偏低"
      },
      {
        "indicatorId": 2,
        "indicatorName": "白细胞计数",
        "valueDecimal": 8.2,
        "valueText": null,
        "isAbnormal": 0,
        "abnormalText": "正常"
      },
      {
        "indicatorId": 5,
        "indicatorName": "血小板计数",
        "valueDecimal": 180.0,
        "valueText": null,
        "isAbnormal": 0,
        "abnormalText": "正常"
      },
      {
        "indicatorId": 9,
        "indicatorName": "C反应蛋白",
        "valueDecimal": 15.3,
        "valueText": null,
        "isAbnormal": 1,
        "abnormalText": "偏高"
      }
    ]
  }
}
```

### 4.4 更新检查记录

`PUT /api/records/{id}`

**请求参数**: 所有字段可选。`values` 数组会全量覆盖原有值。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| examDate | String | 否 | 检查日期时间 |
| hospital | String | 否 | 检查医院 |
| doctor | String | 否 | 主治医生 |
| notes | String | 否 | 备注 |
| imageUrl | String | 否 | 报告照片URL |
| values | Array | 否 | 指标值列表，传了则全量覆盖 |

**请求示例**

```json
{
  "examDate": "2026-08-01T10:00:00",
  "hospital": "市第一人民医院",
  "doctor": "陈主任",
  "notes": "修改备注",
  "values": [
    { "indicatorId": 1, "valueDecimal": 108.0 },
    { "indicatorId": 2, "valueDecimal": 7.5 },
    { "indicatorId": 12, "valueText": "右股骨远端干骺端，较前无变化" }
  ]
}
```

> 更新时 `values` 数组会全量覆盖原有值，不传 `values` 则只更新记录本身的字段。数值型指标传 `valueDecimal`，文本型指标传 `valueText`。

### 4.5 删除检查记录

`DELETE /api/records/{id}`

> 级联删除该记录下的所有指标值。

---

## 五、趋势分析模块

### 5.1 获取指标趋势数据

`GET /api/patients/{patientId}/trends`

> 当前每次查询单个指标的趋势。多指标对比（如同时看血红蛋白和白细胞计数）列为后续迭代功能。

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| indicatorId | Integer | 是 | 指标ID |
| startDate | String | 否 | 起始日期 (yyyy-MM-dd) |
| endDate | String | 否 | 截止日期 (yyyy-MM-dd) |

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "indicator": {
      "id": 1,
      "name": "血红蛋白",
      "unit": "g/L",
      "normalMin": 120.0,
      "normalMax": 160.0
    },
    "points": [
      { "recordId": 1, "examDate": "2026-03-28", "value": 142.0, "isAbnormal": 0 },
      { "recordId": 2, "examDate": "2026-05-20", "value": 125.0, "isAbnormal": 0 },
      { "recordId": 4, "examDate": "2026-08-01", "value": 105.0, "isAbnormal": 2 }
    ]
  }
}
```

### 5.2 获取可查趋势的指标列表

`GET /api/patients/{patientId}/trendable-indicators`

> 返回该患者有历史数据的所有数值型指标，供前端下拉选择。

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    {
      "indicatorId": 1,
      "indicatorName": "血红蛋白",
      "unit": "g/L",
      "categoryId": 1,
      "categoryName": "抽血",
      "recordCount": 5
    },
    {
      "indicatorId": 2,
      "indicatorName": "白细胞计数",
      "unit": "10^9/L",
      "categoryId": 1,
      "categoryName": "抽血",
      "recordCount": 5
    },
    {
      "indicatorId": 1,
      "indicatorName": "血红蛋白",
      "unit": "g/L",
      "categoryId": 1,
      "categoryName": "抽血",
      "recordCount": 3
    }
  ]
}
```

---

## 六、指标管理模块

### 6.1 指标列表

`GET /api/indicators`

> 默认仅返回启用状态（`status=1`）的指标，已禁用的指标不出现。

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryId | Integer | 否 | 按分类筛选 |

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "categoryId": 1,
      "name": "血红蛋白",
      "unit": "g/L",
      "valueType": "number",
      "normalMin": 120.0,
      "normalMax": 160.0,
      "sortOrder": 1,
      "status": 1
    },
    {
      "id": 2,
      "categoryId": 1,
      "name": "白细胞计数",
      "unit": "10^9/L",
      "valueType": "number",
      "normalMin": 4.0,
      "normalMax": 10.0,
      "sortOrder": 2,
      "status": 1
    }
  ]
}
```

### 6.2 新增指标

`POST /api/indicators`

**请求示例**

```json
{
  "categoryId": 1,
  "name": "血清铁蛋白",
  "unit": "ng/mL",
  "valueType": "number",
  "normalMin": 15.0,
  "normalMax": 200.0,
  "sortOrder": 11
}
```

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 17,
    "categoryId": 1,
    "name": "血清铁蛋白",
    "unit": "ng/mL",
    "valueType": "number",
    "normalMin": 15.0,
    "normalMax": 200.0,
    "sortOrder": 11,
    "status": 1
  }
}
```

### 6.3 更新指标

`PUT /api/indicators/{id}`

**请求参数**: 同 6.2，所有字段可选。

### 6.4 禁用/启用指标

`PUT /api/indicators/{id}/status`

> 软删除：将 `status` 设为 0（禁用）或 1（启用）。已有历史数据的指标建议禁用而非物理删除。

**请求示例**

```json
{
  "status": 0
}
```

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": null
}
```

### 6.5 删除指标

`DELETE /api/indicators/{id}`

> 物理删除，慎用。仅无历史数据时可操作，否则返回错误提示先用禁用。

### 6.5 参考区间规则

同一指标在不同性别/年龄段区间不同，规则表用来表达这种差异。

**优先级**：报告照片上的印刷区间（`exam_value.ref_min/ref_max`） > 规则表命中 > `exam_indicator.normal_min/max`。落库时用 `ref_source` 标记来源（`report` / `rule`）。

#### 查询某指标的规则

`GET /api/indicators/{indicatorId}/reference-ranges`

```json
{
  "code": 1,
  "data": [
    { "id": 1, "indicatorId": 1, "gender": 1, "ageMin": 18, "ageMax": null,
      "refMin": 130, "refMax": 175, "refText": "130-175", "source": "成人男性参考区间", "priority": 10 },
    { "id": 2, "indicatorId": 1, "gender": 0, "ageMin": 18, "ageMax": null,
      "refMin": 115, "refMax": 150, "refText": "115-150", "source": "成人女性参考区间", "priority": 10 },
    { "id": 3, "indicatorId": 1, "gender": null, "ageMin": 0, "ageMax": 17,
      "refMin": 110, "refMax": 160, "refText": "110-160", "source": "儿童参考区间", "priority": 10 }
  ]
}
```

#### 新增 / 更新 / 删除

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/reference-ranges` | 新增规则 |
| PUT | `/api/reference-ranges/{id}` | 全量更新（gender/age 传 null 表示不限） |
| DELETE | `/api/reference-ranges/{id}` | 删除规则 |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| indicatorId | Integer | 是 | 指标ID（仅新增时必填） |
| gender | Integer | 否 | null=不限，0=女，1=男 |
| ageMin / ageMax | Integer | 否 | 年龄区间，含端点，null 表示不限 |
| refMin / refMax | BigDecimal | 否 | 参考上下限，至少填一个 |
| refText | String | 否 | 区间原文 |
| source | String | 否 | 来源说明 |
| priority | Integer | 否 | 优先级，越大越优先（默认 0） |

**匹配规则**：性别与年龄需同时命中；在命中的规则里先比 `priority`，再比具体程度（同时限定性别+年龄 &gt; 只限定性别 &gt; 只限定年龄），最后取年龄区间更窄的一条。患者没有出生日期时，限定年龄的规则会被跳过。

---

## 七、文件上传

### 7.1 上传报告照片

`POST /api/files/upload`

**请求格式**: `multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 图片文件 (jpg/png, 最大 10MB) |
| type | String | 否 | 文件类型: report=检查报告, avatar=头像 |

**响应示例**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "url": "/api/files/12",
    "fileName": "report_20260801_001.jpg",
    "fileSize": 1024000
  }
}
```

> 返回的 `url` 是受鉴权保护的读取地址，直接存进 `exam_record.image_url`。
> 图片不再通过静态目录公开访问。

### 7.2 读取报告照片

`GET /api/files/{id}`

**认证**: 是，且仅上传者本人可读。除 `token` 请求头外，也支持 `?token=xxx` 查询参数
（浏览器 `<img>` 标签无法自定义请求头）。

| 参数 | 位置 | 必填 | 说明 |
|------|------|------|------|
| id | path | 是 | 上传接口返回的文件ID |
| token | query | 否 | 仅当无法设置请求头时使用 |

**响应**

- 200：图片二进制流（Content-Type 为 image/png 或 image/jpeg）
- 401：未携带或 token 无效
- 404：文件不存在，**或不属于当前用户**（不泄露文件是否存在）

### 7.3 解析化验单图片（AI 抽取）

`POST /api/parse/report`

**认证**: 是。图片必须是**当前用户自己上传**的，否则返回业务错误。

把图片交给多模态模型抽取指标，**只返回候选值，不落库**。前端把候选填进校对页，用户确认后再调 `POST /api/records`。

**请求示例**

```json
{
  "imageUrl": "/api/files/12",
  "categoryId": 1
}
```

**响应示例（节选）**

```json
{
  "code": 1,
  "data": {
    "categoryId": 1,
    "examDate": "2026-03-28",
    "hospital": "市第一人民医院",
    "engine": "qwen-vl-max-latest",
    "candidates": [
      {
        "rawName": "WBC",
        "indicatorId": 30,
        "indicatorName": "白细胞计数",
        "unit": "10^9/L",
        "rawUnit": "10^9/L",
        "valueDecimal": 3.2,
        "refMin": 4.0,
        "refMax": 10.0,
        "refText": "4.0-10.0",
        "isAbnormal": 2,
        "matchType": "alias",
        "confidence": 0.95,
        "warning": null
      }
    ],
    "unmatched": [
      { "rawName": "嗜酸性粒细胞百分比", "rawValue": "2.1", "rawUnit": "%", "refText": "0.4-8.0" }
    ]
  }
}
```

**字段说明**

| 字段 | 说明 |
|------|------|
| matchType | exact=规范名精确命中，alias=别名命中，contains=包含匹配 |
| confidence | 匹配置信度，供前端弱化显示低置信项 |
| unmatched | 指标库里没有的项，需要用户手动指定或丢弃 |
| warning | 单位换算、数值超出合理范围等提示 |

**模型提供方**：`app.llm.provider` 控制，`mock`（默认，返回固定样本，无需 Key）或 `qwen`（通义千问 VL，需 `DASHSCOPE_API_KEY`）。

---

## 八、数据导出

### 8.1 导出患者检查数据

`GET /api/patients/{patientId}/export`

> 导出该患者的所有检查数据为 Excel 文件（.xlsx），每行一条指标值，包含患者信息、检查日期、分类、指标名、测量值、单位、正常范围、是否异常等字段。

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| startDate | String | 否 | 起始日期 (yyyy-MM-dd) |
| endDate | String | 否 | 截止日期 (yyyy-MM-dd) |

**响应**: 直接返回文件流（`Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`）。

---

## 九、接口汇总

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/login` | 用户名密码登录 | 否 |
| POST | `/api/auth/register` | 注册 | 否 |
| GET | `/api/auth/profile` | 获取用户信息 | 是 |
| PUT | `/api/auth/profile` | 更新用户信息 | 是 |
| POST | `/api/auth/logout` | 登出 | 是 |
| GET | `/api/auth/statistics` | 用户统计信息 | 是 |
| GET | `/api/patients` | 患者列表 | 是 |
| GET | `/api/patients/{id}` | 患者详情 | 是 |
| POST | `/api/patients` | 新增患者 | 是 |
| PUT | `/api/patients/{id}` | 更新患者 | 是 |
| DELETE | `/api/patients/{id}` | 删除患者 | 是 |
| GET | `/api/categories` | 分类列表 | 是 |
| POST | `/api/categories` | 新增分类 | 是 |
| PUT | `/api/categories/{id}` | 更新分类 | 是 |
| DELETE | `/api/categories/{id}` | 删除分类 | 是 |
| GET | `/api/patients/{patientId}/records` | 检查记录列表 | 是 |
| GET | `/api/records/{id}` | 检查记录详情 | 是 |
| POST | `/api/records` | 新增检查记录 | 是 |
| PUT | `/api/records/{id}` | 更新检查记录 | 是 |
| DELETE | `/api/records/{id}` | 删除检查记录 | 是 |
| GET | `/api/patients/{patientId}/trends` | 指标趋势 | 是 |
| GET | `/api/patients/{patientId}/trendable-indicators` | 可查趋势指标 | 是 |
| GET | `/api/indicators` | 指标列表 | 是 |
| POST | `/api/indicators` | 新增指标 | 是 |
| PUT | `/api/indicators/{id}` | 更新指标 | 是 |
| PUT | `/api/indicators/{id}/status` | 禁用/启用指标 | 是 |
| DELETE | `/api/indicators/{id}` | 删除指标 | 是 |
| GET | `/api/indicators/{indicatorId}/reference-ranges` | 指标的参考区间规则 | 是 |
| POST | `/api/reference-ranges` | 新增参考区间规则 | 是 |
| PUT | `/api/reference-ranges/{id}` | 更新参考区间规则 | 是 |
| DELETE | `/api/reference-ranges/{id}` | 删除参考区间规则 | 是 |
| POST | `/api/files/upload` | 上传文件 | 是 |
| GET | `/api/files/{id}` | 读取报告照片（仅上传者） | 是 |
| POST | `/api/parse/report` | 解析化验单图片（AI 抽取） | 是 |
| GET | `/api/patients/{patientId}/export` | 导出患者数据 | 是 |

> 接口总数：**28 个**

---

## 附录: isAbnormal 自动计算逻辑

新增或更新 `exam_value` 时，后端自动执行以下判断：

```
if value_decimal is NULL:
    is_abnormal = 0  (文本型指标默认正常)
elif value_decimal > normal_max:
    is_abnormal = 1  (偏高)
elif value_decimal < normal_min:
    is_abnormal = 2  (偏低)
else:
    is_abnormal = 0  (正常)
```

如果 `normal_min` 或 `normal_max` 为 NULL（如影像类文本指标），则 `is_abnormal` 始终为 0。
