# 技术决策记录

## 2026-06-07：项目初始化
- **决策**：Spring Boot 2.7 + Java 8 而非更高版本
- **原因**：用户现有环境不支持 Java 17+，digital-employee 项目已有成熟模式
- **决策**：Python FastAPI sidecar 而非 Java 内处理数据
- **原因**：pandas 在数据处理方面远超 Java 生态
- **决策**：DeepSeek API 而非 GPT-4/Claude
- **原因**：中文能力强、价格低（~1 元/百万 token）、OpenAI 兼容格式
- **决策**：MySQL 而非 PostgreSQL
- **原因**：用户已有 MySQL 实例 (192.168.11.7:3306)
- **决策**：表名加 `da_` 前缀
- **原因**：使用现有 ryactiviti 数据库，避免与业务表冲突
