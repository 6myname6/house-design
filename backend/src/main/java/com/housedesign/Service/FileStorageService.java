package com.housedesign.Service;

import org.springframework.web.multipart.MultipartFile;

/**
 * FileStorageService —— 文件存储抽象。
 *
 * 由 app.storage.type 决定具体实现：
 *   local（默认）-> LocalFileStorageServiceImpl，落在 ./storage 并走 /files/** 对外
 *   oss          -> OssFileStorageServiceImpl，上传到阿里云 OSS
 * 两者返回值都是可直接访问的公网/本机 URL，调用方无需关心底层介质。
 */
public interface FileStorageService {
    /*
     * 上传文件
     *
     * @param file 前端传来文件
     *
     * @param dir 子目录，如”uploads“ （头像，帖子图），”designs“（设计图）
     *
     * @return 可公开访问的URL
     */
    String upload(MultipartFile file, String dir);

    // 把外部URL的图片转存到本公司存储，返回可公开访问的URL
    String downloadFromUrl(String url, String dir);

}
