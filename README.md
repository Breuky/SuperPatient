# SuperPatient

一个长期记录自己检查检验指标的追踪系统，适合体检、慢病随访等需要持续对比同一指标的场景。核心不是「录入数值 + 折线图」，而是把**非结构化化验单照片**变成**可比较、可追踪的结构化指标**。

> 演示与开发均使用合成/脱敏数据，不含任何真实患者信息。

## 核心链路

```
拍照上传
   │
   ▼
POST /api/files/upload          图片落盘 + 登记归属（仅本人可读）
   │
   ▼
POST /api/parse/report          多模态模型抽取 → JSON → 归一化 → 候选值（不落库）
   │
   ▼
校对页（人在环路）               逐项核对、修正、丢弃；模型不直接写库
   │
   ▼
POST /api/records               结构化入库，参考区间随值快照保存
   │
   ▼
趋势图 + 参考区间带 + 异常标红
```

关键取舍：模型只产出**候选**，落库必须经过用户确认。既能追问准确率，又不由模型承担责任。

## 技术栈

- 后端：Spring Boot 4 / Java 21 / MyBatis / MySQL 8 / JWT
- Web：Vue 3 + Element Plus + ECharts
- 登录：用户名 + 密码（BCrypt），JWT 鉴权
- 抽取：可插拔多模态客户端（`mock` / 通义千问 VL）

## 快速开始

### 1. 数据库

首次初始化（**会 DROP 并重建全部表**，仅限空库）：

```bash
mysql -uroot -p < schema.sql
mysql -uroot -p --default-character-set=utf8mb4 super_patient < db/2026-10-21_upload_file.sql
mysql -uroot -p --default-character-set=utf8mb4 super_patient < db/2026-10-21_extraction_fields.sql
```

已有库只需执行 `db/` 下的迁移脚本（幂等，可重复执行）。

可选：导入**合成**演示数据（6 次血常规、两家医院、参考区间不同），用于演示趋势页的分段参考带：

```bash
mysql -uroot -p --default-character-set=utf8mb4 super_patient < db/demo_trend_data.sql
```

导入后用 `demo_trend / demo1234` 登录即可查看。删除：`DELETE FROM user WHERE username='demo_trend';`

### 2. 本地配置

复制模板并填入真实凭据（该文件不入库）：

```bash
cp backend/application-local.yml.example backend/application-local.yml
```

```yaml
spring:
  datasource:
    username: root
    password: 你的数据库密码
app:
# 登录为用户名 + 密码，无需额外配置；示例账号 demo / demo1234
```

### 3. 启动

```bash
cd backend && mvn spring-boot:run          # http://localhost:8080
cd frontend/Web && npm install && npm run dev   # http://localhost:3000
```

打开 `http://localhost:3000`，用 `demo / demo1234` 登录，或点「注册」创建新账号。

### 4. 切换真实模型

```bash
export LLM_PROVIDER=qwen
export DASHSCOPE_API_KEY=sk-xxx
```

默认 `LLM_PROVIDER=mock`，返回固定血常规样本，用于无 Key 时打通整条链路。

## 几个设计点

**参考区间挂在报告上，不是全局常量。** `exam_value.ref_min/ref_max/ref_text` 保存本次报告印刷的区间，`exam_indicator.normal_min/max` 只是默认值。判定顺序是「报告区间 → 指标默认区间」。同一指标在不同医院、性别、年龄段区间不同，做成全局字段就表达不了。

**抽取分三层，便于评测。** `LlmVisionClient` 只返回模型原始 JSON；schema 校验、别名归一、单位换算在后处理层。这样才能把 prompt 优化、schema 约束、后处理规则拆开做对照实验。

**单位换算连参考区间一起换。** 只换数值不换区间，异常判定会整体错位。

**图片按上传者鉴权。** 报告照片不走静态目录，`GET /api/files/{id}` 校验归属，非本人返回 404 且不泄露存在性。

## 测试

```bash
cd backend && mvn test
```

覆盖：指标名归一（含兄弟指标防误配）、单位归一与换算、抽取全链路、参考区间优先级与性别年龄规则匹配、JWT、越权访问。

## 已知限制

- 仅 Web 端；小程序端代码已移除
- Token 无刷新与吊销机制，12 小时到期后需重新登录
- 登录接口无验证码与限流，注册开放，仅适合自用或演示
- 参考区间规则只覆盖差异明确的几项（血红蛋白、尿酸、肌酐、血沉、碱性磷酸酶、白细胞），其余走指标默认值
- 趋势图的参考带按每次报告区间分段；只有一个数据点的指标看不出阶梯变化
- 无审计日志与数据导出权限控制
