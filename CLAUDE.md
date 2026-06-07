# Data Analyst - Team Operations Guide

> AI 数据分析 SaaS 团队运营手册

## Team-Lead Control Plane

- team-lead = 主会话（我），负责用户对齐、任务分解、阶段转换
- 所有工作通过 teammate 完成，不单独派 subagent

## Team Roster

| Name | Role | Model | Key Capability |
|------|------|-------|---------------|
| backend-dev | Backend Developer | sonnet | Spring Boot + Python FastAPI + AI 集成 |
| frontend-dev | Frontend Developer | sonnet | Vue 3 + Element Plus + ECharts |
| researcher | Explorer/Researcher | sonnet | 代码调研 + 技术方案探索（只读） |
| reviewer | Code Reviewer | sonnet | 安全/质量/性能审查（只读） |
| e2e-tester | E2E Tester | sonnet | Playwright 测试 + 浏览器自动化 |

## 项目结构

```
data-analyst/
├── backend/          # Spring Boot 2.7 (Java 8)
├── python-service/   # FastAPI + pandas
├── frontend/         # Vue 3 + Element Plus + ECharts
├── .plans/           # 团队规划文件
├── docker-compose.yml
└── init-db.sql
```

## 技术栈

- 后端: Spring Boot 2.7.18 + Java 8
- 数据处理: Python FastAPI + pandas
- 前端: Vue 3 + Vite + Element Plus + ECharts
- AI: DeepSeek API (OkHttp 调用)
- 数据库: MySQL (192.168.11.7:3306/ryactiviti, 表名 da_ 前缀)
- 部署: Docker Compose

## 当前优先级

Phase 1: 修复已知 Bug（chart 生成、前端上传、数据传递）
Phase 2: 功能完善（文件 ID 返回、端到端测试、代码审查）
Phase 3: 新功能（用户认证、报告导出、付费系统）

## 沟通协议

| 操作 | 方式 |
|------|------|
| 分配任务 | SendMessage(to: "xxx", message: "...") |
| 广播 | SendMessage(to: "*", message: "...") |
| 请求审查 | 开发者直接联系 reviewer |
| 查看状态 | TaskList / 读取 progress.md |
