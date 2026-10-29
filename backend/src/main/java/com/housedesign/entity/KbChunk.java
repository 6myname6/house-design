package com.housedesign.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 知识库分块实体，对应表 t_kb_chunk。
 *
 * <p>定位：分块正文的权威存储，保存在 MySQL；对应的向量写入 PostgreSQL
 * 的 kb_chunk_vector，二者通过 chunk_id 关联。</p>
 *
 * <p>同一文档多次重建索引时，旧版本分块保留但 is_active=0，
 * 检索只使用当前版本的有效分块。</p>
 */
@Data
@TableName("t_kb_chunk")
public class KbChunk {

    /** 分块 ID（主键，自增），同时作为向量表 kb_chunk_vector.chunk_id */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属文档（逻辑外键 → t_kb_document.id） */
    private Long documentId;

    /** 文档版本号，与 t_kb_document.version 对齐 */
    private Integer version;

    /** 文档内分块序号，从 0 开始，用于重建索引时保持顺序 */
    private Integer chunkNo;

    /** 所属章节标题；用于引用展示与关键词检索加权，可为空 */
    private String heading;

    /** 分块正文（TEXT），检索命中后作为上下文拼给模型 */
    private String content;

    /** 分块内容 SHA-256，用于块级去重，避免同一段内容重复入库 */
    private String contentHash;

    /** 估算 token 数，用于控制上下文预算 */
    private Integer tokenCount;

    /** 字符数，便于中文分块参数调优 */
    private Integer charCount;

    /**
     * 是否为当前版本的有效分块。
     * true：参与检索；false：历史版本，仅保留追溯。
     */
    private Boolean isActive;

    /** 创建时间（数据库默认 CURRENT_TIMESTAMP） */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
