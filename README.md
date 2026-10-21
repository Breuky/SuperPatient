# SuperPatient

一个长期记录自己检查检验指标的追踪系统，适合体检、慢病随访这类需要**持续对比同一指标**的场景。

核心不是「录入数值 + 折线图」，而是把**非结构化化验单照片**变成**可比较、可追踪的结构化指标**。

> 演示与开发均使用合成数据，不含任何真实个人健康信息。

## 核心链路

```
拍照上传
   │
   ▼
POST /api/files/upload      图片落盘 + 登记归属（仅上传者本人可读）
   │
   ▼
POST /api/parse/report      多模态模型抽取 → JSON → 归一化 → 候选值（不落库）
   │
   ▼
校对页（人在环路）            逐项核对、修正、丢弃；模型不直接写库
   │
   ▼
POST /api/records           结构化入库，参考区间随值快照保存
   │
   ▼
趋势图 + 分段参考区间带 + 异常标红
```

关键取舍：模型只产出**候选**，落库必须经过用户确认。既能追问准确率，又不由模型承担责任。

## 功能

| 模块 | 说明 |
|------|------|
| 账号 | 用户名 + 密码注册/登录，BCrypt 存储，JWT 鉴权 |
| 记录 | 按分类录入检查记录，数值型/文本型指标，自动判定偏高偏低 |
| 识别 | 上传化验单照片 → 模型抽取 → 校对页确认 → 入库 |
| 趋势 | 单个指标多次复查折线，参考区间按每次报告分段绘制 |
| 指标管理 | 自定义分类与指标，维护别名（用于抽取归一）与性别/年龄参考区间规则 |
| 图片 | 报告照片按上传者鉴权访问，不走公开静态目录 |

## 技术栈

- 后端：Spring Boot 4 / Java 21 / MyBatis / MySQL 8 / JWT
- Web：Vue 3 + Element Plus + ECharts（Vite 构建）
- 抽取：可插拔多模态客户端，`mock`（默认，无需 Key）/ 通义千问 VL
- 密码：`spring-security-crypto` 的 BCrypt（未引入完整 Spring Security）

## 快速开始

**环境**：JDK 21、Maven 3.9+、MySQL 8、Node 18+

### 1. 初始化数据库

首次初始化会 **DROP 并重建全部表**：

```bash
mysql -uroot -p --default-character-set=utf8mb4 < schema.sql
```

`schema.sql` 已包含全部表结构与种子数据（7 张表：user / exam_category / exam_indicator / exam_record / exam_value / reference_range / upload_file），**不需要再执行 `db/` 下的迁移脚本** —— 那些只用于升级已有库。

可选：导入合成演示数据（6 次血常规、两家医院、参考区间不同），用于演示趋势页的分段参考带：

```bash
mysql -uroot -p --default-character-set=utf8mb4 super_patient < db/demo_trend_data.sql
```

### 2. 本地配置

数据库密码不写在仓库里。复制模板并填入：

```bash
cp backend/application-local.yml.example backend/application-local.yml
```

```yaml
spring:
  datasource:
    username: root
    password: 你的数据库密码
```

`application-local.yml` 已被 `.gitignore` 排除。也可以用环境变量 `DB_USERNAME` / `DB_PASSWORD` 覆盖。

### 3. 启动

```bash
cd backend && mvn spring-boot:run        # http://localhost:8080
cd frontend/Web && npm install && npm run dev   # http://localhost:3000
```

打开 `http://localhost:3000`：

| 账号 | 密码 | 用途 |
|------|------|------|
| `demo` | `demo1234` | 基础示例数据 |
| `demo_trend` | `demo1234` | 6 次复查的趋势演示（需先导入 demo 数据） |

也可以点「注册」创建新账号，注册时需选择性别（用于匹配参考区间）。

### 4. 切换真实模型

默认 `LLM_PROVIDER=mock`，返回固定样本，用于无 Key 时打通链路。

```bash
export LLM_PROVIDER=qwen
export DASHSCOPE_API_KEY=sk-xxx
# 可选：LLM_MODEL / LLM_BASE_URL
```

## 几个设计点

**用户即患者本人。** 一个账号就是一份健康档案：性别、出生日期直接挂在 `user` 上，检查记录用 `user_id` 外键。没有"账号→患者→记录"的中间层，归属校验也就退化成一次字段比较。

**参考区间分三级，而不是一个全局常量。** 判定顺序：

```
报告照片上印的区间 (exam_value.ref_*)   ← 最可信，落库时快照
        ↓ 没有
性别/年龄规则命中 (reference_range)      ← 规则表，按 priority → 具体度 → 区间窄度排序
        ↓ 没有
指标默认值 (exam_indicator.normal_*)     ← 兜底，不写快照，便于以后调整
```

同一指标在不同医院、性别、年龄段区间都不同，做成全局字段就表达不了。落库时用 `ref_source` 记录本次用的是哪种来源。

**抽取分三层，便于评测。** `LlmVisionClient` 只返回模型原始 JSON；schema 校验、别名归一、单位换算都在后处理层。这样才能把 prompt 优化、schema 约束、后处理规则拆开做对照实验。

**单位换算连参考区间一起换。** 只换数值不换区间，异常判定会整体错位。

**人在环路。** 模型输出只是候选，用户逐项确认后才写库。低置信度、别名匹配、单位换算都会在校对页标出来。

**图片不走静态目录。** `GET /api/files/{id}` 校验上传者归属，非本人返回 404 不泄露存在性；鉴权支持请求头或 `?token=`（`<img>` 无法带请求头）。

## 接口文档

见 [api.md](api.md)，包含全部接口、请求响应示例与字段说明。启动后可访问 Swagger UI：`http://localhost:8080/swagger-ui.html`

## 测试

```bash
cd backend && mvn test
```

37 个用例，覆盖：

- 指标名归一（含「总胆红素 / 直接胆红素」这类兄弟指标防误配）
- 单位归一与换算（含换算后区间同步）
- 抽取全链路（别名命中、未匹配项、被代码块包裹的 JSON、非法 imageUrl）
- 参考区间优先级与性别/年龄规则匹配
- 账号注册校验、密码哈希、登录错误提示一致性
- JWT 签发解析、越权访问

## 目录结构

```
SuperPatient/
├── backend/                 Spring Boot 服务
│   ├── src/main/java/com/superpatient/
│   │   ├── controller/      接口层
│   │   ├── service/         业务层
│   │   ├── mapper/          MyBatis Mapper
│   │   ├── entity/ dto/     数据模型
│   │   ├── extract/         指标匹配、单位归一、提示词
│   │   ├── llm/             可插拔多模态客户端
│   │   └── config/ interceptor/ util/
│   └── src/test/java/       单元测试
├── db/                      迁移脚本 + 演示数据
├── frontend/Web/            Vue 3 前端
├── schema.sql               完整建库脚本
├── api.md                   接口文档
└── README.md
```

## 已知限制

- 仅 Web 端，无移动端
- Token 无刷新与吊销机制，12 小时到期后需重新登录
- 登录接口无验证码与限流，注册开放，仅适合自用或演示
- 参考区间规则只覆盖差异明确的几项（血红蛋白、尿酸、肌酐、血沉、碱性磷酸酶、白细胞），其余走指标默认值
- 趋势图的参考带按每次报告区间分段；只有一个数据点的指标看不出阶梯变化
- 模型没有真实样本的准确率评测数据（当前默认 mock，未接入真实 Key）
- 无审计日志与数据导出权限控制
