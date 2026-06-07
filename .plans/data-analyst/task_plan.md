# Data Analyst - 主任务计划

## 项目概述
AI 数据分析 SaaS：上传 Excel/CSV → AI 自动分析 + 图表 → 自然语言问答
技术栈：Spring Boot 2.7 (Java 8) + Python FastAPI + Vue 3 + DeepSeek API + MySQL

## 当前状态
MVP 已搭建完成，存在以下已知问题需要修复和完善。

## Phase 1：修复已知 Bug（优先）

### P1-1 [backend] Chart 数据自动生成失败
- **问题**：AnalysisService.generateChartData() 调用 Python 执行代码时，生成的代码引用了未定义的变量
- **文件**：`backend/src/main/java/com/dataanalyst/service/AnalysisService.java:60-75`
- **期望**：上传文件后，4 个建议图表都能正确生成数据
- **状态**：pending
- **负责人**：backend-dev

### P1-2 [frontend] el-upload 组件自动弹出文件选择器
- **问题**：页面加载时 el-upload drag 模式自动触发 file chooser
- **文件**：`frontend/src/views/Upload.vue:8-23`
- **期望**：页面正常加载，用户点击或拖拽才触发文件选择
- **状态**：pending
- **负责人**：frontend-dev

### P1-3 [frontend] 分析页面通过 query 传递数据，URL 过长
- **问题**：大文件分析结果通过 URL query param 传递，可能超长
- **文件**：`frontend/src/views/Upload.vue:102`, `frontend/src/views/Analysis.vue:60-65`
- **期望**：通过文件 ID 从后端获取分析结果
- **状态**：pending
- **负责人**：frontend-dev

## Phase 2：功能完善

### P2-1 [backend] 文件上传返回文件 ID
- **期望**：上传接口返回文件 ID，前端用 ID 跳转
- **状态**：pending
- **负责人**：backend-dev

### P2-2 [e2e] 端到端测试
- **期望**：Playwright 测试覆盖上传→分析→问答完整流程
- **状态**：pending
- **负责人**：e2e-tester

### P2-3 [reviewer] 代码审查
- **期望**：审查所有现有代码，提出改进建议
- **状态**：pending
- **负责人**：reviewer

## Phase 3：新功能（后续）

### P3-1 用户认证（JWT）
### P3-2 PDF/PPT 报告导出
### P3-3 付费系统（微信/支付宝）
### P3-4 多文件项目管理
