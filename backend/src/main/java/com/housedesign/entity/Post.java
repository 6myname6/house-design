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
 * 帖子实体，对应表 t_post。
 * images 为图片 URL 列表，DB 中存 JSON 字符串（TEXT），经 JacksonTypeHandler 与 List&lt;String&gt; 互转。
 * 表结构见 backend/src/main/resources/db/schema.sql。
 */
@Data
@TableName(value = "t_post", autoResultMap = true)
public class Post {

    /** 帖子 ID（PK，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作者（逻辑外键 → t_user.id） */
    private Long userId;

    /** 帖子正文 */
    private String content;

    /** 图片 URL 列表（JSON 字符串存储） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    /** 点赞数（冗余计数器，DB 默认 0） */
    private Integer likeCount;

    /** 评论数（冗余计数器，DB 默认 0） */
    private Integer commentCount;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
