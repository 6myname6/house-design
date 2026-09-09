package com.housedesign.dto.request;

import java.util.List;

import lombok.Data;

/**
 * 评论请求体：content（文字）与 images（图片 URL 列表）至少填一个。
 */
@Data
public class CommentRequest {
    /** 评论文字（与 images 至少填一个） */
    private String content;

    /** 评论图片 URL 列表（可选） */
    private List<String> images;
}
