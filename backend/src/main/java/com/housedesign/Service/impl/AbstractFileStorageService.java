package com.housedesign.Service.impl;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.web.reactive.function.client.WebClient;

import com.housedesign.Service.FileStorageService;

/**
 * 文件存储公共基类：把「存到哪里」无关的逻辑收敛在一处。
 *
 * 子类只负责真正的落库动作（本地磁盘 / 阿里云 OSS），
 * 校验规则、命名规则、远程下载这些"存储介质无关"的部分都放这里，
 * 保证两种后端的行为完全一致（换成 OSS 后上传体验不变）。
 */
public abstract class AbstractFileStorageService implements FileStorageService {

    // 白名单：只允许图片扩展名，防止被上传 .js/.html 之类的可执行内容
    protected static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    // 扩展名 -> MIME：本地存储由 Tomcat 按扩展名自动推断，OSS 必须手工塞进 ObjectMetadata，
    // 否则浏览器拿到 application/octet-stream 会把图片当成附件下载而不是直接显示
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp");

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
     */
    protected byte[] downloadBytes(String url) {
        byte[] bytes = WebClient.create(url).get().retrieve()
                .bodyToMono(byte[].class)
                .block(Duration.ofSeconds(60));
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("下载内容为空:" + url);
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
