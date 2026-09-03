package com.housedesign.entity;

/**
 * AI 生成任务状态机（对应 t_generated_model.status，VARCHAR 按枚举名存储）：
 * PENDING → PROCESSING → SUCCESS / FAILED
 */
public enum GenerationStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED
}
