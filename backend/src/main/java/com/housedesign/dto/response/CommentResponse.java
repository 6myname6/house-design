package com.housedesign.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 评论响应体：作者信息由 Service 层连 User 表补全。
 */
@Data
public class CommentResponse {
    /** 评论 ID */
    private Long id;

    /** 所属帖子 ID */
    private Long postId;

    /** 评论者 ID */
    private Long userId;

    /** 被回复的评论 ID（NULL=顶层评论；非NULL=回复） */
    private Long parentId;

    /** 评论者昵称（Service 层补全） */
    private String authorName;

    /** 评论者头像（Service 层补全） */
    private String authorAvatar;

    /** 评论文字 */
    private String content;

    /** 评论图片 URL 列表 */
    private List<String> images;

    /** 点赞数（Service 层补全） */
    private Integer likeCount;

    /** 当前用户是否已赞（Service 层补全） */
    private Boolean likedByMe;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
