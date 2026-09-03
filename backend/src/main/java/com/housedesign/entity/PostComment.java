package com.housedesign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子评论实体（扁平结构），对应表 t_post_comment。
 * 表结构见 backend/src/main/resources/db/schema.sql。
 */
@Data
@TableName("t_post_comment")
public class PostComment {

    /** 评论 ID（PK，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属帖子（逻辑外键 → t_post.id） */
    private Long postId;

    /** 评论者（逻辑外键 → t_user.id） */
    private Long userId;

    /** 评论内容 */
    private String content;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
