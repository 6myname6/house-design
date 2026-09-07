package com.housedesign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设计项目实体，对应表 t_design_project。
 * 表结构见 backend/src/main/resources/db/schema.sql。
 */
@Data
@TableName("t_design_project")
public class DesignProject {

    /** 项目 ID（PK，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户（逻辑外键 → t_user.id） */
    private Long userId;

    /** 项目名称 */
    private String name;

    /** 项目描述 */
    private String description;

    /** 用户自定义风格要求（自由文本，如「原木色、温馨、多绿植」） */
    private String style;

    /** 预设风格 code（DesignStyle 枚举 code，如 modern-minimalist；与 style 至少填一个） */
    private String styleLabel;

    /** 设计图相对路径（storage 下） */
    private String designImagePath;

    /** 设计图可访问 URL */
    private String designImageUrl;

    /** 创建时间（数据库 DEFAULT CURRENT_TIMESTAMP 自动生成，不可更新） */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间（数据库 ON UPDATE CURRENT_TIMESTAMP 自动维护） */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
