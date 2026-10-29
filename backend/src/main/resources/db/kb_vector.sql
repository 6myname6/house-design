-- Active: 1793086196423@@127.0.0.1@5432
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS kb_chunk_vector (
    chunk_id BIGINT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    version INT NOT NULL,
    embedding vector (1024) NOT NULL,
    model VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_kb_vec_doc ON kb_chunk_vector (document_id, version);

CREATE INDEX IF NOT EXISTS idx_kb_vec_hnsw ON kb_chunk_vector USING hnsw (embedding vector_cosine_ops);