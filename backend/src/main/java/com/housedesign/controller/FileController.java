package com.housedesign.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.housedesign.Service.FileStorageService;
import com.housedesign.common.Result;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;

@RequestMapping("/api/files")
@RequiredArgsConstructor
@RestController
@Slf4j
public class FileController {
    private final FileStorageService fileStorageService;

    // 文件上传
    @PostMapping("/upload")
    public Result<String> postMethodName(@RequestParam("file") MultipartFile file) {
        // 文件初步非空校验
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的图片");
        }
        log.info("文件开始上传。。。");
        return Result.success(fileStorageService.upload(file, "uploads"));
    }

}
