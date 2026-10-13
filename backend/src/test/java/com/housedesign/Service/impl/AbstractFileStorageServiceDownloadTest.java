package com.housedesign.Service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import com.sun.net.httpserver.HttpServer;

/**
 * 验证 AI 图片下载三道防线（修复计划第 9 项）：
 * ①codec 20MB 缓冲（>256KB 不再炸 DataBufferLimitException）；
 * ②Content-Type 非 image/* 拒绝；
 * ③字节数超配置上限拒绝。
 */
class AbstractFileStorageServiceDownloadTest {

    private static HttpServer server;
    private static int port;

    /** 仅用于暴露 protected downloadBytes 的最小子类 */
    static class TestStorage extends AbstractFileStorageService {
        TestStorage(long maxDownloadBytes) {
            super(maxDownloadBytes);
        }

        @Override
        public String upload(MultipartFile file, String dir) {
            return null;
        }

        @Override
        public String downloadFromUrl(String url, String dir) {
            return null;
        }

        byte[] download(String url) {
            return downloadBytes(url);
        }
    }

    @BeforeAll
    static void startMockServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();

        // 300KB 伪 JPEG：超过 WebClient 默认 256KB 缓冲
        server.createContext("/image", ex -> {
            byte[] data = new byte[300 * 1024];
            new Random(1).nextBytes(data);
            ex.getResponseHeaders().set("Content-Type", "image/jpeg");
            ex.sendResponseHeaders(200, data.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(data);
            }
        });
        // 非图片内容
        server.createContext("/text", ex -> {
            byte[] data = "<html>not an image</html>".getBytes();
            ex.getResponseHeaders().set("Content-Type", "text/html");
            ex.sendResponseHeaders(200, data.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(data);
            }
        });
        // 404（临时链接失效）
        server.createContext("/missing", ex -> ex.sendResponseHeaders(404, -1));
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    void downloadsImageLargerThan256Kb() {
        byte[] bytes = new TestStorage(20L * 1024 * 1024)
                .download("http://localhost:" + port + "/image");
        assertEquals(300 * 1024, bytes.length);
        assertTrue(bytes.length > 256 * 1024, "旧默认缓冲 256KB 会在此抛 DataBufferLimitException");
    }

    @Test
    void rejectsNonImageContentType() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new TestStorage(20L * 1024 * 1024)
                        .download("http://localhost:" + port + "/text"));
        assertTrue(ex.getMessage().contains("不是图片"), ex.getMessage());
    }

    @Test
    void failsOn404() {
        assertThrows(IllegalStateException.class,
                () -> new TestStorage(20L * 1024 * 1024)
                        .download("http://localhost:" + port + "/missing"));
    }

    @Test
    void rejectsBytesOverConfiguredLimit() {
        // 上限仅 1KB，300KB 图片必须被拦截
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new TestStorage(1024L)
                        .download("http://localhost:" + port + "/image"));
        assertTrue(ex.getMessage().contains("超出大小限制"), ex.getMessage());
    }
}
