# data-analyst — AI 数据分析平台

上传 Excel/CSV 文件，自动分析数据、生成图表，支持自然语言问答。

## 技术栈

| 服务 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + ECharts |
| 后端 | Spring Boot 2.7.18 (Java 8) + MySQL |
| AI 服务 | Python FastAPI + 数据分析引擎 |
| 部署 | Docker Compose + Nginx |

## 项目结构

```
data-analyst/
├── frontend/           # Vue 3 前端
│   └── src/
│       ├── views/      # Projects, Upload, Analysis, ProjectDetail
│       ├── components/ # 图表组件
│       ├── api/        # 接口封装
│       └── router/     # 路由配置
├── backend/            # Spring Boot 后端
│   └── src/main/java/  # REST API, 数据管理
├── python-service/     # FastAPI AI 服务
│   ├── main.py         # API 入口
│   ├── profiler.py     # 数据画像
│   └── executor.py     # 分析执行
├── docker-compose.yml  # 容器编排
├── nginx.conf          # Nginx 配置
└── init-db.sql         # 数据库初始化
```

## 功能

- **文件上传** — 支持 Excel/CSV 格式
- **自动分析** — 数据画像、统计摘要、异常检测
- **图表生成** — ECharts 自动可视化
- **自然语言问答** — 用中文提问，AI 分析数据并回答
- **项目管理** — 多项目、多文件管理

## 快速开始

### Docker Compose（推荐）
```bash
docker-compose up -d
# 前端: http://localhost:80
# 后端 API: http://localhost:8080
# Python 服务: http://localhost:8001
```

### 本地开发

**前端：**
```bash
cd frontend
npm install
npm run dev
```

**后端：**
```bash
cd backend
mvn spring-boot:run
```

**Python 服务：**
```bash
cd python-service
pip install -r requirements.txt
uvicorn main:app --port 8001
```

## 环境变量

| 变量 | 说明 |
|------|------|
| MYSQL_HOST | MySQL 地址 |
| MYSQL_PORT | MySQL 端口（默认 3306）|
| MYSQL_DATABASE | 数据库名 |
| MYSQL_USER | 数据库用户 |
| MYSQL_PASSWORD | 数据库密码 |
| OPENAI_API_KEY | AI 服务 API Key |
