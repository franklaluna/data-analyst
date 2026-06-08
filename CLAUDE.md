# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AI 数据分析 SaaS — 用户上传 Excel/CSV，系统自动分析数据、生成图表，支持自然语言问答。

## Commands

### Full Stack (Docker)
```bash
docker compose up --build          # 启动全部服务: python:8001, backend:8080, frontend:80
```

### Backend (Spring Boot 2.7 + Java 8)
```bash
cd backend
mvn spring-boot:run                 # 启动开发服务器 (port 8080)
mvn test                            # 运行全部测试
mvn test -Dtest=AnalysisServiceTest # 运行单个测试
mvn clean package -DskipTests -B    # 打包
```

### Frontend (Vue 3 + Vite)
```bash
cd frontend
npm run dev                         # 启动开发服务器 (port 3000, 代理 /api → localhost:8080)
npm run build                       # 类型检查 + 生产构建 (vue-tsc && vite build)
npm run preview                     # 预览生产构建
```

### Python Service (FastAPI)
```bash
cd python-service
pip install -r requirements.txt     # 安装依赖
uvicorn main:app --host 0.0.0.0 --port 8001  # 启动服务
```

## Architecture

三服务架构，通过 HTTP 通信：

```
Frontend (Vue 3, port 3000/80)
  │ axios, base=/api
  ▼
Backend (Spring Boot, port 8080)
  ├── PythonClient (OkHttp) ──→ Python Service (FastAPI, port 8001)
  │     POST /analyze  → profiler.py (pandas 数据分析)
  │     POST /execute  → executor.py (沙箱执行生成的代码)
  ├── LLMService (OkHttp) ───→ DeepSeek API (/v1/chat/completions)
  └── JdbcTemplate ──────────→ MySQL (192.168.11.7:3306/ryactiviti)
```

**上传流程**: 文件上传 → Python 分析 → LLM 生成摘要 → 生成图表数据 → 存入 `da_files` 表
**问答流程**: 用户提问 → LLM 生成 pandas 代码 → Python 沙箱执行 → LLM 总结结果 → 返回答案+图表

### Backend Package Structure

```
com.dataanalyst
├── controller/    REST 控制器 (FileController, QAController, ProjectController)
├── service/       业务逻辑 (AnalysisService, QAService, LLMService, PythonClient, ProjectService)
├── model/         POJO (FileInfo, QARecord, Project)
└── config/        配置 (LLMConfig — OkHttpClient bean, DeepSeek/Python URL)
```

**关键模式**:
- 构造器注入，无 `@Autowired`
- `JdbcTemplate` 直接写 SQL，无 ORM
- `PythonClient` 用 OkHttp 调用 Python 服务
- `LLMService` 用 OkHttp 调用 DeepSeek API（OpenAI 兼容格式）
- `executor.py` 在子进程中运行代码，10 秒超时，关键字黑名单 + dunder 过滤

### Database

MySQL `192.168.11.7:3306/ryactiviti`，表名 `da_` 前缀。Schema 在 `init-db.sql`。

### Frontend

- Vue 3 Composition API (`<script setup lang="ts">`)
- Element Plus (中文 locale) + ECharts
- 路由: `/` (项目列表) → `/projects/:id` (项目详情) → `/upload` (上传) → `/analysis/:id` (分析)
- API 客户端集中在 `src/api/client.ts`
- Vite 开发代理: `/api` → `localhost:8080`

### Configuration

- `backend/src/main/resources/application.yml` — 数据库连接、LLM 配置、Python 服务地址
- `.env` — `DEEPSEEK_API_KEY`, `DB_USERNAME`, `DB_PASSWORD`（参考 `.env.example`）
- `frontend/vite.config.ts` — 开发服务器端口、代理配置
- `nginx.conf` — 生产环境反向代理

## Team Operations

### Team Roster

| Name | Role | Model | Key Capability |
|------|------|-------|---------------|
| backend-dev | Backend Developer | sonnet | Spring Boot + Python FastAPI + AI 集成 |
| frontend-dev | Frontend Developer | sonnet | Vue 3 + Element Plus + ECharts |
| researcher | Explorer/Researcher | sonnet | 代码调研 + 技术方案探索（只读） |
| reviewer | Code Reviewer | sonnet | 安全/质量/性能审查（只读） |
| e2e-tester | E2E Tester | sonnet | Playwright 测试 + 浏览器自动化 |

### Communication Protocol

| 操作 | 方式 |
|------|------|
| 分配任务 | SendMessage(to: "xxx", message: "...") |
| 广播 | SendMessage(to: "*", message: "...") |
| 请求审查 | 开发者直接联系 reviewer |
| 查看状态 | TaskList / 读取 progress.md |
| 恢复团队 | 读取 `.plans/data-analyst/team-snapshot.md` |

### Team File Structure

```
.plans/data-analyst/
  task_plan.md        -- 主计划
  team-snapshot.md    -- 团队快照（用于恢复）
  decisions.md        -- 决策日志
  docs/               -- 知识库 (architecture, api-contracts, index)
  backend-dev/        -- 后端任务目录
  frontend-dev/       -- 前端任务目录
  researcher/         -- 调研目录
  e2e-tester/         -- 测试目录
  reviewer/           -- 审查目录
```
