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

/**
 * 文生图服务：对接阿里云百炼专属实例的 DashScope 协议（base-url 指向 {host}/api/v1）。
 * wanx 系列模型为异步任务：POST /services/aigc/text2image/image-synthesis 提交，
 * 轮询 GET /tasks/{task_id} 直至 SUCCEEDED，取 output.results[0].url（临时链接，调用方转存 OSS）。
 */
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
        // 1.提交异步任务（X-DashScope-Async: enable 必带，否则同步模式直接超时）
        JsonNode submit;
        try {
            submit = webClient.post()
                    .uri("/services/aigc/text2image/image-synthesis")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("X-DashScope-Async", "enable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(java.util.Map.of(
                            "model", imageModel,
                            "input", java.util.Map.of("prompt", prompt),
                            // 尺寸用 x 分隔（本地配置习惯），DashScope 要求 * 分隔；n=1 只出一张，避免默认 4 张浪费额度
                            "parameters", java.util.Map.of("size", size.replace('x', '*'), "n", 1)))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(timeoutSeconds));
        } catch (WebClientResponseException e) {
            throw classifyHttpError(e, "百炼提交生成");
        } catch (WebClientRequestException e) {
            // 连不上/握手失败/断网等 IO 类故障：值得重试
            log.warn("百炼提交生成网络异常", e);
            throw new AiImageException("百炼网络连接失败，请稍后重试", true, null, e);
        } catch (IllegalStateException e) {
            // block 超时抛 IllegalStateException("Timeout on blocking read...")：可重试
            throw classifyBlockingError(e, "百炼提交生成");
        }
        String taskId = submit.path("output").path("task_id").asText(null);
        if (taskId == null || taskId.isBlank()) {
            log.warn("百炼提交生成响应缺少 task_id：{}", truncate(submit.toString(), 500));
            throw new AiImageException("AI生成失败:提交任务无 task_id", false, null, null);
        }
        log.info("百炼提交生成成功：model={}, taskId={}", imageModel, taskId);

        // 2.轮询异步结果,直到SUCCEEDED或超时
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
                        .uri("/tasks/" + taskId)
                        .header("Authorization", "Bearer " + apiKey)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block(Duration.ofSeconds(30));
            } catch (WebClientResponseException e) {
                throw classifyHttpError(e, "百炼查询结果");
            } catch (WebClientRequestException e) {
                log.warn("百炼查询结果网络异常：taskId={}", taskId, e);
                throw new AiImageException("百炼网络连接失败，请稍后重试", true, null, e);
            } catch (IllegalStateException e) {
                throw classifyBlockingError(e, "百炼查询结果");
            }
            JsonNode output = result.path("output");
            String status = output.path("task_status").asText("");
            log.info("轮询生成状态：taskId={}, status={}", taskId, status);
            if ("SUCCEEDED".equalsIgnoreCase(status)) {
                // results[0].url 为 24h 有效的临时链接，调用方负责转存 OSS
                return output.path("results").get(0).path("url").asText();
            }
            if ("FAILED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status)
                    || "UNKNOWN".equalsIgnoreCase(status)) {
                // 上游业务失败（审核拒绝/内容违规等），重试无意义
                String detail = output.path("message").asText("");
                String code = output.path("code").asText("");
                throw new AiImageException("AI生成失败:" + (code.isEmpty() ? detail : code + " " + detail),
                        false, null, null);
            }
            // PENDING / RUNNING 继续等
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
            Long seconds = Long.parseLong(headerValue.trim());
            return Math.min(seconds, 30) * 1000L;
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    // 截断过长的响应体，避免塞满 error_message 列（VARCHAR(1024)）
    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
