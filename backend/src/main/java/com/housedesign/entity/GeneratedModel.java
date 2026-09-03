package com.housedesign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 生成任务与结果实体，对应表 t_generated_model。
 * 表结构见 backend/src/main/resources/db/schema.sql。
 */
@Data
@TableName("t_generated_model")
public class GeneratedModel {

    /** 任务 ID（PK，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目（逻辑外键 → t_design_project.id） */
    private Long projectId;

    /** 发起用户（逻辑外键 → t_user.id） */
    private Long userId;

    /** 状态（按枚举名存 VARCHAR）：PENDING/PROCESSING/SUCCESS/FAILED */
    private GenerationStatus status;

    /** AI 提供方（mock / zhipu） */
    private String provider;

    /** 3D 模型（glb/gltf）URL */
    private String modelUrl;

    /** 预览缩略图 URL */
    private String previewImageUrl;

    /** 全景图 URL（智谱出图时填充） */
    private String panoramaUrl;

    /** 3D 场景 JSON（LONGTEXT，程序化渲染用） */
    private String sceneConfig;

    /** 失败原因 */
    private String errorMessage;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
