package com.housedesign.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 点赞/取消点赞的响应（帖子点赞与评论点赞共用）。
 */
@Data
@AllArgsConstructor
public class LikeResult {
    /** 点赞后的总数 */
    private int likeCount;

    /** true=当前用户已赞；false=已取消 */
    private boolean liked;
}
