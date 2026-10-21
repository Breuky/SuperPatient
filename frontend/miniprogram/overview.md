# SuperPatient 小程序前端 — 开发完成

## 概览

按接口文档完整实现了 6 个页面的微信小程序原生代码，覆盖全部 28 个 API 接口的调用。

## 项目位置

```
C:\Shizexi\ProjectLearning\SuperPatient\frontend\miniprogram\
```

## 文件统计

| 类型 | 数量 |
|------|------|
| JS 逻辑 | 10 |
| WXML 模板 | 6 |
| WXSS 样式 | 7 |
| JSON 配置 | 8 |
| 工具类 | 3 |
| 图标资源 | 6 |
| **合计** | **40** |

## 6 个页面功能覆盖

| 页面 | 功能 | 对应 API |
|------|------|----------|
| **首页** (index) | 患者列表、搜索、新增患者弹窗 | patients (list/create) |
| **患者详情** (patient-detail) | 信息卡片、分类筛选、记录列表、编辑/删除/导出 | patients (detail/update/delete), categories, records (list) |
| **新增检查** (record-add) | 分类动态切换、动态指标表单、拍照上传报告 | categories, records (create), files/upload |
| **检查详情** (record-detail) | 异常状态标记、编辑模式、删除 | records (detail/update/delete) |
| **趋势图表** (trend) | 患者+指标选择、Canvas 折线图、数据列表 | patients, trends, trendable-indicators |
| **个人中心** (profile) | 个人信息、统计数据、头像上传、登出 | auth (profile/statistics/logout) |

## 开始使用

1. 用**微信开发者工具**打开 `C:\Shizexi\ProjectLearning\SuperPatient\frontend\miniprogram\`
2. 在 `project.config.json` 中将 `appid` 改为你的小程序 AppID
3. 启动后端 Spring Boot 项目（端口 8080）
4. 开发者工具 → 详情 → 勾选"不校验合法域名"
5. 点击编译即可预览

## 后续待做

- 替换 TabBar 图标为实际设计稿（当前为纯色占位块）
- 后端接口联调测试
- 趋势页可扩展支持多指标对比折线
