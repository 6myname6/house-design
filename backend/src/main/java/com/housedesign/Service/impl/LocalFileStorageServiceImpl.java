package com.housedesign.Service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.housedesign.Service.FileStorageService;

import org.springframework.beans.factory.annotation.Value;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LocalFileStorageServiceImpl implements FileStorageService {
    // 1.从yml读配置，避免硬编码
    @Value("${app.storage.public-base-url}")
    private String publicBaseUrl;

    @Value("${app.storage.location}")
    private String storageLocation;

    // 白名单：只允许图片扩展名
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Override
    public String upload(MultipartFile file, String dir) {
        // 2.校验扩展名
        String original = file.getOriginalFilename();// 获取初始名
        String extension = getExtension(original);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 jpg/png/gif/webp 格式图片");
        }
        // 3.UUID重命名：防重命名覆盖+防中文乱码+防路径穿越
        String storedName = UUID.randomUUID() + "." + extension;
        // 4.落盘到./storage/<dir>/
        try {
            Path directory = Paths.get(storageLocation, dir)
                    .toAbsolutePath().normalize();
            Files.createDirectories(directory);
            Path target = directory.resolve(storedName);
            file.transferTo(target.toFile());
        } catch (Exception e) {
            log.error("文件上传失败：{}", original, e);
            throw new IllegalStateException("文件上传失败，请稍后重试", e);
        }
        // 5.拼出可访问url返回，如http://localhost:8080/files/uploads/***
        String url = publicBaseUrl + "/" + dir + "/" + storedName;
        log.info("文件上传成功：{}->{}", original, url);
        return url;
    }

    // 获取拓展名方法
    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
