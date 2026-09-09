package com.housedesign.Service;

import org.springframework.web.multipart.MultipartFile;

/**
 * FileStorageService
 */
public interface FileStorageService {
    /*
     * 上传文件
     * 
     * @param file 前端传来文件
     * 
     * @param dir 子目录，如”uploads“ （头像，帖子图），”designs“（设计图）
     * 
     * @param 可公开访问的URL
     */
    String upload(MultipartFile file, String dir);

}
