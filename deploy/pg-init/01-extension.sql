-- RAG 向量库初始化：仅建扩展，业务表由 backend 侧脚本/迁移维护
-- 说明：docker-entrypoint-initdb.d 只在数据卷首次初始化时执行；已有 pg-data 卷需手动执行一次
CREATE EXTENSION IF NOT EXISTS vector;
