package com.housedesign.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.housedesign.entity.GenerationStatus;

import lombok.Data;

@Data
public class GenerationResponse {

    /** 生成任务 ID */
    private Long id;

    /** 所属项目 ID */
    private Long projectId;

    /** 任务状态：PENDING/PROCESSING/SUCCESS/FAILED（枚举序列化为枚举名） */
    private GenerationStatus status;

    /** 实际使用的 AI 提供方（mock / zhipu） */
    private String provider;

    /** 3D 模型（glb/gltf）URL，mock 模式为空 */
    private String modelUrl;

    /** 预览缩略图 URL */
    private String previewImageUrl;

    /** 首张全景/效果图 URL（智谱模式填充） */
    private String panoramaUrl;

    /** 3D 场景描述 JSON 字符串（前端按 type 渲染） */
    private String sceneConfig;

    /** 失败原因（429/额度/Key 错误等） */
    private String errorMessage;

    /** 创建时间 */
    @JsonFormat
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat
    private LocalDateTime updatedAt;
}