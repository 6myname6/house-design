package com.housedesign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子评论实体（扁平结构），对应表 t_post_comment。
 * images 为图片 URL 列表，DB 中存 JSON 字符串（TEXT），经 JacksonTypeHandler 与 List&lt;String&gt; 互转。
 * 表结构见 backend/src/main/resources/db/schema.sql。
 */
@Data
@TableName(value = "t_post_comment", autoResultMap = true)
public class PostComment {

    /** 评论 ID（PK，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属帖子（逻辑外键 → t_post.id） */
    private Long postId;

    /** 评论者（逻辑外键 → t_user.id） */
    private Long userId;

    /** 被回复的评论 ID（NULL=顶层评论；非NULL=该评论的回复） */
    private Long parentId;

    /** 评论文字（与 images 至少填一个） */
    private String content;

    /** 评论图片 URL 列表（JSON 字符串存储） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
