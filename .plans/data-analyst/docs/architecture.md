# 系统架构

## 整体架构

```
Browser (Vue 3 + Element Plus + ECharts)
    |
    v
Nginx (反向代理 + 静态文件)
    |
    |--- /api/* --> Spring Boot :8080
    |                 |                  |
    |                 | HTTP             | OkHttp
    |                 v                  v
    |           Python FastAPI      DeepSeek API
    |           :8001
    |
    |--- /*    --> Vue SPA
```

## 核心流程

### 文件上传 + AI 分析
1. 浏览器上传文件 → `POST /api/files/upload`
2. Spring Boot 保存文件到 `/tmp/data-analyst-uploads/`
3. 调用 Python `POST /analyze` → pandas profiling
4. 调用 DeepSeek 生成分析摘要
5. 生成图表数据（调用 Python 执行 pandas 代码）
6. 存入 MySQL `da_files` 表
7. 返回结果给前端

### 自然语言问答
1. 用户提问 → `POST /api/qa` (fileId + question)
2. 从 DB 取文件 schema + 样本数据
3. DeepSeek 生成 pandas 代码
4. Python 沙箱执行代码
5. DeepSeek 总结结果
6. 存入 `da_qa_history`
7. 返回答案 + 图表

## 关键技术决策
- Spring Boot 负责编排，Python 负责数据处理，LLM 负责智能
- 代码执行沙箱：subprocess + 10s 超时 + 禁止危险操作
- 文件限制：50MB，超过 10 万行取前 5 万行分析
