package com.housedesign.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

/**
 * RAG 向量模型（Embedding）接入配置。
 *
 * <p>复用百炼的 OpenAI 兼容协议，模型默认 text-embedding-v3；
 * 输出维度必须与 PostgreSQL 的 vector(1024) 一致，否则写向量表会失败。</p>
 */
@Configuration
public class RagEmbeddingConfig {

    /**
     * 构建 EmbeddingModel Bean。
     *
     * @param baseUrl        兼容模式根地址（SDK 会自动追加 /embeddings）
     * @param apiKey         百炼 API Key，与聊天模型共用 BAILIAN_API_KEY
     * @param model          模型名，写入 kb_chunk_vector.model（长度 ≤ 64）
     * @param dimension      输出向量维度，必须与 vector(1024) 对齐
     * @param timeoutSeconds 单次调用超时秒数，防止请求无限挂起
     * @return Embedding 客户端（线程安全，可复用）
     */
    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${app.rag.embedding.base-url}") String baseUrl,
            @Value("${app.rag.embedding.api-key}") String apiKey,
            @Value("${app.rag.embedding.model}") String model,
            @Value("${app.rag.embedding.dimension:1024}") int dimension,
            @Value("${app.rag.embedding.timeout-seconds:30}") long timeoutSeconds) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .dimensions(dimension)                        // 1024 维，与 pgvector 表对齐
                .timeout(Duration.ofSeconds(timeoutSeconds))  // 超时兜底
                .maxRetries(2)                                // 瞬时抖动自动重试
                .build();
    }
}
