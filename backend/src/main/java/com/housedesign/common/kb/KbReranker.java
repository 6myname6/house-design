package com.housedesign.common.kb;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

/**
 * 知识库重排器：KbReranker
 * 包含Rerank模型的调用，用于对搜索召回的候选文档进行重排
 */
@Slf4j
@Component
public class KbReranker {
    private static final String RERANK_PATH = "/services/rerank/text-rerank/text-rerank";
    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;

    /** 重排结果：候选下标+相关度分数（0-1） */
    public record Result(int index, double score) {
    }

    public KbReranker(WebClient.Builder builder,
            @Value("${app.rag.rerank.base-url}") String baseUrl,
            @Value("${app.rag.rerank.api-key}") String apiKey,
            @Value("${app.rag.rerank.model:gte-rerank-v2}") String model,
            @Value("${app.rag.rerank.timeout-seconds:10}") int timeoutSeconds) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * 对搜索召回的候选文档进行重排
     * 
     * @param query     查询文本
     * @param documents 候选文档列表
     * @return 重排结果列表
     */
    public List<Result> rerank(String query, List<String> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        try {
            JsonNode root = webClient.post().uri(RERANK_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "model", model,
                            "input", Map.of("query", query, "documents", documents),
                            "parameters", Map.of("return_documents", false, "top_n", documents.size())))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(timeoutSeconds));
            JsonNode results = root.path("output").path("results");
            if (!results.isArray()) {
                log.warn("Rerank响应缺少output.results");
                return List.of();
            }
            List<Result> ranked = new ArrayList<>(results.size());
            for (JsonNode item : results) {
                int index = item.path("index").asInt(-1);
                double score = item.path("relevance_score").asDouble(-1);
                if (index >= 0 && index < documents.size() && score >= 0) {
                    ranked.add(new Result(index, score));
                }
            }
            return ranked;
        } catch (Exception e) {
            log.warn("Rerank调用失败，降级为纯向量检索", e);
            return List.of();
        }
    }
}
