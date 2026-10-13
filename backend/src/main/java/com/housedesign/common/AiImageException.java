package com.housedesign.common;

/**
 * AI 文生图调用异常：携带"是否可重试"语义。
 *
 * 429 限流 / 5xx 上游故障 / 超时 / 网络 IO → retryable=true（指数退避重试）；
 * 400 参数非法 / 401 Key 错误 / 403 欠费等 4xx → retryable=false（立即失败，重试只会放大限流）。
 */
public class AiImageException extends RuntimeException {

    private final boolean retryable;

    /** 上游 Retry-After 建议等待毫秒数（429 响应头携带），无则 null */
    private final Long retryAfterMillis;

    public AiImageException(String message, boolean retryable, Long retryAfterMillis, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
        this.retryAfterMillis = retryAfterMillis;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Long getRetryAfterMillis() {
        return retryAfterMillis;
    }
}
