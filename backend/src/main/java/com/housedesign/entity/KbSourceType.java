package com.housedesign.entity;

/**
 * 知识库文档来源类型（对应 t_kb_document.source_type，按枚举名存储）。
 */
public enum KbSourceType {

    /** 上传文件（PDF / Word / Markdown 等） */
    FILE,

    /** 外部网页链接 */
    URL,

    /** 直接录入的纯文本 */
    TEXT
}
