package com.housedesign.Service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import lombok.extern.slf4j.Slf4j;

/**
 * 本地磁盘存储实现（type=local，默认）。
 *
 * 文件落在 ./storage/<dir>/ 下，由 WebConfig 把 /files/** 映射出去对外访问。
 * 仅适合单机开发/演示：多实例部署时各机器磁盘不共享，A 机器上传的图 B 机器访问不到。
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageServiceImpl extends AbstractFileStorageService {

    // 1.从yml读配置，避免硬编码
    @Value("${app.storage.public-base-url}")
    private String publicBaseUrl;

    @Value("${app.storage.location}")
    private String storageLocation;

    @Override
    public String upload(MultipartFile file, String dir) {
        // 2.校验扩展名 + UUID 重命名：防重名覆盖 + 防中文乱码 + 防路径穿越
        String original = file.getOriginalFilename();
        String storedName = buildStoredName(original);
        // 3.落盘到./storage/<dir>/
        Path directory = resolveDirectory(dir);
        try {
            Files.createDirectories(directory);
            file.transferTo(directory.resolve(storedName).toFile());
        } catch (Exception e) {
            log.error("文件上传失败：{}", original, e);
            throw new IllegalStateException("文件上传失败，请稍后重试", e);
        }
        // 4.拼出可访问url返回，如http://localhost:8080/files/uploads/***
        String url = publicBaseUrl + "/" + dir + "/" + storedName;
        log.info("文件上传成功：{}->{}", original, url);
        return url;
    }

    // 将生成的效果图下载持久化
    @Override
    public String downloadFromUrl(String url, String dir) {
        // 1.用WebClient把URL的图片下载成字节数组(block转同步等待)
        byte[] bytes = downloadBytes(url);

        // 2.从URL最后一段取扩展名并校验，再UUID重命名
        String storedName = buildStoredName(fileNameFromUrl(url));
        // 3.落盘
        Path directory = resolveDirectory(dir);
        try {
            Files.createDirectories(directory);
            // 注意：必须用 storedName 落盘，返回的 URL 也是 storedName，
            // 用原始路径名写、用新名读会 404
            Files.write(directory.resolve(storedName), bytes);
        } catch (Exception e) {
            log.error("外部图片下载失败:{}", url, e);
            throw new IllegalStateException("AI生图下载失败,请稍后重试");
        }

        // 4.拼本地可访问URL
        String localURL = publicBaseUrl + "/" + dir + "/" + storedName;
        log.info("外部图片持久化成功:{}->{}", url, localURL);
        return localURL;
    }

    /**
     * 解析出 ./storage/<dir> 的绝对路径并 normalize。
     *
     * normalize 是为了把 ../ 之类的片段折叠掉，防止 dir 传入 "../../" 时写到项目目录之外。
     */
    private Path resolveDirectory(String dir) {
        return Paths.get(storageLocation, dir).toAbsolutePath().normalize();
    }
}
