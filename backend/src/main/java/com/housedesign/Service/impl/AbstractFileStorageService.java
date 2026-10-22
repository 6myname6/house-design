package com.housedesign.Service.impl;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.housedesign.Service.FileStorageService;

import lombok.extern.slf4j.Slf4j;

/**
 * 文件存储公共基类：把「存到哪里」无关的逻辑收敛在一处。
 *
 * 子类只负责真正的落库动作（当前唯一实现为阿里云 OSS），
 * 校验规则、命名规则、远程下载这些"存储介质无关"的部分都放这里。
 */
@Slf4j
public abstract class AbstractFileStorageService implements FileStorageService {

    // 白名单：只允许图片扩展名，防止被上传 .js/.html 之类的可执行内容
    protected static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    // 扩展名 -> MIME：OSS 必须手工塞进 ObjectMetadata，
    // 否则浏览器拿到 application/octet-stream 会把图片当成附件下载而不是直接显示
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp");

    /** WebClient 解码内存上限：默认只有 256KB，1440x720 JPEG 会触发 DataBufferLimitException */
    private static final int MAX_IN_MEMORY_BYTES = 20 * 1024 * 1024;

    // 全应用共享一个 WebClient（内部带连接池）；下载地址每次通过 .uri(url) 显式指定
    private static final WebClient HTTP = WebClient.builder()
            .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(configurer -> configurer.defaultCodecs()
                            .maxInMemorySize(MAX_IN_MEMORY_BYTES))
                    .build())
            .build();

    /** 下载图片字节数上限（codec 20MB 之外的第二道保险，配置外部化，由子类构造传入） */
    private final long maxDownloadBytes;

    protected AbstractFileStorageService(long maxDownloadBytes) {
        this.maxDownloadBytes = maxDownloadBytes;
    }

    /**
     * 取扩展名并校验白名单，不合法直接抛异常。
     *
     * @param fileName 原始文件名或 URL 路径
     * @return 小写扩展名（已在白名单内）
     */
    protected String requireAllowedExtension(String fileName) {
        String extension = getExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 jpg/png/gif/webp 格式图片");
        }
        return extension;
    }

    /**
     * 按原始文件名生成落库用的新名字：UUID + 原扩展名。
     *
     * 三个作用：①防止同名覆盖 ②规避中文/特殊字符乱码 ③杜绝路径穿越（原文件名不参与拼路径）
     *
     * @param originalFileName 原始文件名（可来自上传，也可来自外部 URL 的最后一段）
     * @return 形如 3f2a...c1.png
     */
    protected String buildStoredName(String originalFileName) {
        String extension = requireAllowedExtension(originalFileName);
        return UUID.randomUUID() + "." + extension;
    }

    /** 按落库文件名推断 Content-Type，未知则退回二进制流 */
    protected String contentTypeOf(String storedName) {
        return CONTENT_TYPES.getOrDefault(getExtension(storedName), "application/octet-stream");
    }

    /**
     * 把外部 URL 的内容下载成字节数组（AI 出图的临时链接 -> 我们要转存成自己的永久地址）。
     *
     * WebClient.block() 同步等待，调用点都在异步线程池里，不会阻塞 Tomcat 请求线程。
     * 三道防线：①codec 内存上限 20MB ②Content-Type 必须 image/* ③字节数不超过配置上限。
     */
    protected byte[] downloadBytes(String url) {
        ResponseEntity<byte[]> response;
        try {
            // 用 java.net.URI 重载：String 重载会走 UriTemplate 再编码，把签名里的 %2F 变 %252F 导致 OSS 403
            response = HTTP.get().uri(URI.create(url)).retrieve()
                    .toEntity(byte[].class)
                    .block(Duration.ofSeconds(60));
        } catch (WebClientResponseException e) {
            // 上游明确返回 4xx/5xx（含图片临时链接失效 404）
            log.warn("下载外部图片失败：HTTP {}，url={}", e.getStatusCode().value(), url);
            throw new IllegalStateException("图片下载失败（HTTP " + e.getStatusCode().value() + "），请稍后重试", e);
        } catch (RuntimeException e) {
            // 连接失败/读超时/响应超 20MB 解码上限（DataBufferLimitException）都落这里
            log.warn("下载外部图片异常：url={}", url, e);
            throw new IllegalStateException("图片下载失败或图片过大，请稍后重试", e);
        }
        if (response == null || response.getBody() == null || response.getBody().length == 0) {
            throw new IllegalStateException("下载内容为空：" + url);
        }
        // 只接受图片响应：URL 后缀可伪造，Content-Type 是服务端实际产出的内容类型
        MediaType contentType = response.getHeaders().getContentType();
        if (contentType == null || !"image".equalsIgnoreCase(contentType.getType())) {
            log.warn("下载地址返回非图片内容，已拒绝转存：contentType={}, url={}", contentType, url);
            throw new IllegalStateException("下载地址返回的不是图片内容，已拒绝转存");
        }
        byte[] bytes = response.getBody();
        if (bytes.length > maxDownloadBytes) {
            log.warn("下载图片超出字节上限：{} > {}，url={}", bytes.length, maxDownloadBytes, url);
            throw new IllegalStateException(
                    "图片超出大小限制（" + (maxDownloadBytes / 1024 / 1024) + "MB）");
        }
        return bytes;
    }

    /** 取 URL 路径的最后一段（如 https://x.com/a/b.png -> b.png），供白名单校验使用 */
    protected String fileNameFromUrl(String url) {
        try {
            String path = new URI(url).getPath();
            if (path == null) {
                throw new IllegalArgumentException("非法图片地址:" + url);
            }
            int slash = path.lastIndexOf('/');
            return slash >= 0 ? path.substring(slash + 1) : path;
        } catch (Exception e) {
            throw new IllegalArgumentException("非法图片地址:" + url, e);
        }
    }

    // 获取扩展名
    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
