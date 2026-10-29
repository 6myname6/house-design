package com.housedesign.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * RAG 向量库连接配置（PostgreSQL + pgvector）。
 * 仅存向量，业务文本权威仍在 MySQL。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.rag.vector-store")
public class PgVectorProperties {

    private String url;

    private String username;

    private String password;

    private int maximumPoolSize = 5;

    private long connectionTimeoutMs = 3000;
}
