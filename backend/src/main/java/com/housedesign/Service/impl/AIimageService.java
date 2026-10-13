package com.housedesign.Service.impl;

import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.housedesign.common.AiImageException;

import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AIimageService {
    private final WebClient webClient;
    private final String apiKey;
    private final String imageModel;
    private final String defaultImageSize;
    private final int timeoutSeconds;

    public AIimageService(WebClient.Builder builder,
            @Value("${app.ai.base-url}") String baseUrl,
            @Value("${app.ai.api-key}") String apiKey,
            @Value("${app.ai.image-model}") String imageModel,
            @Value("${app.ai.room-image-size:1440x720}") String defaultImageSize,
            @Value("${app.ai.timeout-seconds}") int timeoutSeconds) {
        webClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.imageModel = imageModel;
        this.defaultImageSize = defaultImageSize;
        this.timeoutSeconds = timeoutSeconds;
    }

    // 文生图（使用配置的默认尺寸，照片漫游为 1440x720 宽幅）:发起任务并阻塞轮询,返回临时图片url
    public String generateImageUrl(String prompt) {
        return generateImageUrl(prompt, defaultImageSize);
    }

    // 文生图（显式指定尺寸，如 1440x720 / 1024x1024）:发起任务并阻塞轮询,返回临时图片url
    public String generateImageUrl(String prompt, String size) {
        // 1.发起生成任务
        JsonNode submit;
        try {
            submit = webClient.post()
                    .uri("/images/generations")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(java.util.Map.of("model", imageModel, "prompt", prompt, "size", size))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(timeoutSeconds));
        } catch (WebClientResponseException e) {
            throw classifyHttpError(e, "智谱发起生成");
        } catch (WebClientRequestException e) {
            // 连不上/握手失败/断网等 IO 类故障：值得重试
            log.warn("智谱发起生成网络异常", e);
            throw new AiImageException("智谱网络连接失败，请稍后重试", true, null, e);
        } catch (IllegalStateException e) {
            // block 超时抛 IllegalStateException("Timeout on blocking read...")：可重试
            throw classifyBlockingError(e, "智谱发起生成");
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
            } catch (WebClientResponseException e) {
                throw classifyHttpError(e, "智谱查询结果");
            } catch (WebClientRequestException e) {
                log.warn("智谱查询结果网络异常：taskId={}", taskId, e);
                throw new AiImageException("智谱网络连接失败，请稍后重试", true, null, e);
            } catch (IllegalStateException e) {
                throw classifyBlockingError(e, "智谱查询结果");
            }
            String status = result.path("task_status").asText("");
            log.info("轮询生成状态：taskId={}, status={}, 响应={}", taskId, status, truncate(result.toString(), 300));
            if ("SUCCESS".equalsIgnoreCase(status)) {
                return result.path("data").get(0).path("url").asText();
            }
            if ("FAIL".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status)) {
                // 上游业务失败（审核拒绝/内容违规等），重试无意义
                throw new AiImageException("AI生成失败:" + result.path("error").asText(), false, null, null);
            }
            // 兜底：若响应里已带 url（部分模型终态直接给图），提前返回
            String url = extractDataUrl(result);
            if (url != null) {
                return url;
            }
        }
        // 轮询超过 timeoutSeconds：任务可能仍在排队，重试可能成功
        throw new AiImageException("AI生成超时(" + timeoutSeconds + "s)，请稍后重试", true, null, null);
    }

    /**
     * 按 HTTP 状态码分类：429 限流 / 5xx 上游故障可重试（429 透传 Retry-After）；
     * 其余 4xx（400 参数非法、401 Key 错、403 欠费等）立即失败，重试只会放大限流。
     */
    private AiImageException classifyHttpError(WebClientResponseException e, String phase) {
        int status = e.getStatusCode().value();
        boolean retryable = status == 429 || status >= 500;
        Long retryAfterMillis = parseRetryAfter(e.getHeaders().getFirst("Retry-After"));
        String body = e.getResponseBodyAsString();
        String detail = (body == null || body.isBlank()) ? e.getMessage() : truncate(body, 300);
        log.warn("{}被拒绝：status={}, retryable={}, body={}", phase, status, retryable, detail);
        return new AiImageException(phase + "失败(HTTP " + status + ")：" + detail,
                retryable, retryAfterMillis, e);
    }

    /** block 超时（消息以 "Timeout on blocking" 开头）判为可重试，其余非法状态原样抛出 */
    private RuntimeException classifyBlockingError(IllegalStateException e, String phase) {
        if (e.getMessage() != null && e.getMessage().startsWith("Timeout on blocking")) {
            log.warn("{}阻塞等待超时", phase);
            return new AiImageException(phase + "超时，请稍后重试", true, null, e);
        }
        return e;
    }

    /**
     * 解析 Retry-After：仅支持 delta-seconds 形式（HTTP-date 形式返回 null，由调用方走默认退避）。
     * 上限夹在 30s，避免上游要求等几分钟时长时间占着生成线程。
     */
    private Long parseRetryAfter(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        try {
            long seconds = Long.parseLong(headerValue.trim());
            return Math.min(seconds, 30) * 1000L;
        } catch (NumberFormatException ignore) {
            return null;
        }
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
