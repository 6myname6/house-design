package com.housedesign.entity;

/**
 * 知识库文档入库状态机（对应 t_kb_document.status，按枚举名存储）：
 * PENDING → PARSING → INDEXING → READY / FAILED。
 * 失败后可重新发起入库，状态回到 PENDING。
 */
public enum KbDocumentStatus {

    /** 已创建文档记录，等待入库任务领取 */
    PENDING,

    /** 正在解析原始文件（PDF / Word / Markdown 等） */
    PARSING,

    /** 正在分块 + 生成向量并写入索引 */
    INDEXING,

    /** 入库完成，可被检索 */
    READY,

    /** 入库失败，error_message 记录原因，可重试 */
    FAILED
}
