package com.housedesign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子点赞实体，对应表 t_post_like。
 * 表结构见 backend/src/main/resources/db/schema.sql。
 */
@Data
@TableName("t_post_like")
public class PostLike {

    /** 主键（PK，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被赞帖子（逻辑外键 → t_post.id） */
    private Long postId;

    /** 点赞用户（逻辑外键 → t_user.id） */
    private Long userId;

    /** 点赞时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
