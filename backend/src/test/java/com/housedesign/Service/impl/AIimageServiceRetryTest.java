package com.housedesign.Service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.housedesign.common.AiImageException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * 验证第 11 项错误分类：429/5xx 可重试（429 透传 Retry-After），其余 4xx 立即失败。
 * generateImageUrl 固定 POST /images/generations，故每个用例起独立 mock 端口。
 */
class AIimageServiceRetryTest {

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void clientError401IsNotRetryable() throws Exception {
        prepareServer(401, null);
        AIimageService svc = newService();
        AiImageException ex = assertThrows(AiImageException.class,
                () -> svc.generateImageUrl("测试提示词"));
        assertFalse(ex.isRetryable(), "401（Key 错误）不应重试");
    }

    @Test
    void rateLimit429IsRetryableAndCarriesRetryAfter() throws Exception {
        prepareServer(429, "2");
        AIimageService svc = newService();
        AiImageException ex = assertThrows(AiImageException.class,
                () -> svc.generateImageUrl("测试提示词"));
        assertTrue(ex.isRetryable(), "429 限流应可重试");
        assertEquals(2000L, ex.getRetryAfterMillis());
    }

    @Test
    void serverError500IsRetryable() throws Exception {
        prepareServer(500, null);
        AIimageService svc = newService();
        AiImageException ex = assertThrows(AiImageException.class,
                () -> svc.generateImageUrl("测试提示词"));
        assertTrue(ex.isRetryable(), "5xx 上游故障应可重试");
    }

    private void prepareServer(int status, String retryAfter) throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/images/generations", (HttpExchange ex) -> {
            ex.getResponseHeaders().set("Content-Type", "application/json");
            if (retryAfter != null) {
                ex.getResponseHeaders().set("Retry-After", retryAfter);
            }
            byte[] body = "{\"error\":{\"code\":\"mock\"}}".getBytes();
            ex.sendResponseHeaders(status, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
    }

    private AIimageService newService() {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        return new AIimageService(WebClient.builder(), baseUrl,
                "test-key", "cogview-3-flash", "1024x1024", 10);
    }
}
