package com.housedesign.dto.request;

import java.util.List;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评论请求体：content（文字）与 images（图片 URL 列表）至少填一个。
 */
@Data
public class CommentRequest {
    /** 评论文字（与 images 至少填一个） */
    @Size(max = 2000, message = "内容不能超过2000字")
    private String content;

    /** 评论图片 URL 列表（可选） */
    @Size(max = 9, message = "最多上传9张图片")
    private List<String> images;
}
