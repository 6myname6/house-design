package com.housedesign.Service.impl;

import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AIimageService {
    private final WebClient webClient;
    private final String apiKey;
    private final String imageModel;
    private final int timeoutSeconds;

    public AIimageService(WebClient.Builder builder,
            @Value("${app.ai.base-url}") String baseUrl,
            @Value("${app.ai.api-key}") String apiKey,
            @Value("${app.ai.image-model}") String imageModel,
            @Value("${app.ai.timeout-seconds}") int timeoutSeconds) {
        webClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.imageModel = imageModel;
        this.timeoutSeconds = timeoutSeconds;
    }

    // 文生图:发起任务并阻塞轮询,返回临时图片url
    public String generateImageUrl(String prompt) {
        // 1.发起生成任务
        JsonNode submit;
        try {
            submit = webClient.post()
                    .uri("/images/generations")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(java.util.Map.of("model", imageModel, "prompt", prompt, "size", "1024x1024"))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(timeoutSeconds));
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            // 智谱返回 4xx/5xx 时，把响应体（真实原因）带进异常消息，方便排查
            String body = e.getResponseBodyAsString();
            log.error("智谱发起生成被拒绝：status={}, body={}", e.getStatusCode(), body);
            throw new IllegalStateException("智谱发起生成失败(" + e.getStatusCode() + ")："
                    + (body == null || body.isBlank() ? e.getMessage() : truncate(body, 300)));
        }
        String taskId = submit.path("id").asText(null);
        String firstUrl = extractDataUrl(submit);
        // 兼容"发起即出图"的模型（如 cogview-3-flash）：响应直接带 data[0].url，无需轮询
        if (firstUrl != null) {
            log.info("智谱发起即出图：model={}, url已返回", imageModel);
            return firstUrl;
        }
        log.info("智谱发起生成成功(需轮询)：taskId={}, 完整响应={}", taskId, truncate(submit.toString(), 500));

        // 2.轮询异步结果,直到SUCCESS或超时
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                throw new IllegalStateException("生成任务被中断", e);
            }
            JsonNode result;
            try {
                result = webClient.get()
                        .uri("/async-result/" + taskId)
                        .header("Authorization", "Bearer " + apiKey)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block(Duration.ofSeconds(30));
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                String body = e.getResponseBodyAsString();
                log.error("智谱查询结果被拒绝：status={}, body={}", e.getStatusCode(), body);
                throw new IllegalStateException("智谱查询结果失败(" + e.getStatusCode() + ")："
                        + (body == null || body.isBlank() ? e.getMessage() : truncate(body, 300)));
            }
            String status = result.path("task_status").asText("");
            log.info("轮询生成状态：taskId={}, status={}, 响应={}", taskId, status, truncate(result.toString(), 300));
            if ("SUCCESS".equalsIgnoreCase(status)) {
                return result.path("data").get(0).path("url").asText();
            }
            if ("FAIL".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status)) {
                throw new IllegalStateException("AI生成失败:" + result.path("error").asText());
            }
            // 兜底：若响应里已带 url（部分模型终态直接给图），提前返回
            String url = extractDataUrl(result);
            if (url != null) {
                return url;
            }
        }
        throw new IllegalStateException("AI生成超时(" + timeoutSeconds + "s)");
    }

    // 从响应 data[0].url 提取图片 URL，无则返回 null
    private String extractDataUrl(JsonNode node) {
        JsonNode data = node == null ? null : node.path("data");
        if (data != null && data.isArray() && data.size() > 0 && data.get(0).hasNonNull("url")) {
            return data.get(0).path("url").asText();
        }
        return null;
    }

    // 截断过长的响应体，避免塞满 error_message 列（VARCHAR(1024)）
    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
