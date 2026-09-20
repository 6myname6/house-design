package com.housedesign.Service.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * 阿里云 OSS 存储实现（type=oss 时装配）。
 *
 * 与本地实现的差别：文件不再落在服务器磁盘，而是 putObject 到 Bucket，
 * 返回 https://{bucket}.{endpoint}/{dir}/{uuid}.{ext} 这样的公网地址。
 * 好处是多实例部署时不用共享磁盘/做 NFS，且图片走 CDN、不占应用服务器带宽。
 *
 * 开关：STORAGE_TYPE=oss（环境变量），密钥 OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET。
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.storage.type", havingValue = "oss")
public class OssFileStorageServiceImpl extends AbstractFileStorageService {

    @Value("${app.storage.oss.endpoint}")
    private String endpoint;

    @Value("${app.storage.oss.bucket}")
    private String bucket;

    @Value("${app.storage.oss.public-base-url}")
    private String publicBaseUrl;

    @Value("${app.storage.oss.access-key-id}")
    private String accessKeyId;

    @Value("${app.storage.oss.access-key-secret}")
    private String accessKeySecret;

    // OSSClient 是线程安全的，全应用共用一个实例即可（内部维护 HTTP 连接池，切勿每次上传 new 一个）
    private OSS ossClient;

    /**
     * 启动即建连：密钥没配就直接启动失败，避免等到用户上传时才报错（fail-fast）。
     */
    @PostConstruct
    void init() {
        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret)) {
            throw new IllegalStateException(
                    "storage.type=oss 但缺少密钥，请注入环境变量 OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET");
        }
        ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        log.info("OSS 存储已启用：endpoint={}, bucket={}", endpoint, bucket);
    }

    /** 容器关闭时释放连接池，否则进程退出前会有一批连接悬挂 */
    @PreDestroy
    void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS 客户端已关闭");
        }
    }

    @Override
    public String upload(MultipartFile file, String dir) {
        // 1.校验扩展名 + UUID 重命名（复用基类规则，与本地存储行为一致）
        String original = file.getOriginalFilename();
        String storedName = buildStoredName(original);
        // 2.Object Key：OSS 里没有"目录"，dir/ 只是 key 的前缀，控制台会按 / 展示成文件夹
        String key = dir + "/" + storedName;

        try (InputStream in = file.getInputStream()) {
            ossClient.putObject(bucket, key, in, buildMetadata(storedName, file.getSize()));
        } catch (Exception e) {
            log.error("OSS 上传失败：{}", original, e);
            throw new IllegalStateException("文件上传失败，请稍后重试", e);
        }

        String url = publicBaseUrl + "/" + key;
        log.info("文件上传成功：{}->{}", original, url);
        return url;
    }

    @Override
    public String downloadFromUrl(String url, String dir) {
        // 1.把外部链接（AI 出图的临时地址）下载成字节
        byte[] bytes = downloadBytes(url);

        // 2.从 URL 最后一段取扩展名并做白名单校验，再 UUID 重命名
        String storedName = buildStoredName(fileNameFromUrl(url));
        String key = dir + "/" + storedName;

        try {
            ossClient.putObject(bucket, key, new ByteArrayInputStream(bytes),
                    buildMetadata(storedName, bytes.length));
        } catch (OSSException | ClientException e) {
            // OSSException：服务端返回了错误码（无权限/桶不存在/限流）；ClientException：网络层失败
            log.error("外部图片转存 OSS 失败:{}，错误码={}", url, e instanceof OSSException oss ? oss.getErrorCode() : "N/A", e);
            throw new IllegalStateException("AI生图下载失败,请稍后重试");
        }

        String publicUrl = publicBaseUrl + "/" + key;
        log.info("外部图片持久化成功:{}->{}", url, publicUrl);
        return publicUrl;
    }

    /**
     * 组装对象元信息。
     *
     * contentLength 必须显式设置：用 InputStream 上传时 SDK 无法预知长度，
     * 不设置会退化成 chunked 传输，部分场景（大图）容易失败。
     */
    private ObjectMetadata buildMetadata(String storedName, long contentLength) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        // 不设 Content-Type，浏览器/前端 <img> 会把它当附件下载
        metadata.setContentType(contentTypeOf(storedName));
        // 文件名是 UUID，内容永不变更 -> 可以放心长缓存，省流量
        metadata.setCacheControl("public, max-age=31536000");
        return metadata;
    }
}
