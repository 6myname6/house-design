package com.housedesign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论点赞实体，对应表 t_comment_like。
 * UNIQUE(comment_id, user_id) 保证同一用户对同一评论只能点赞一次。
 */
@Data
@TableName("t_comment_like")
public class CommentLike {

    /** 主键（PK，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被赞评论（逻辑外键 → t_post_comment.id） */
    private Long commentId;

    /** 点赞用户（逻辑外键 → t_user.id） */
    private Long userId;

    /** 点赞时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
