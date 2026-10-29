package com.housedesign.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 知识库文档实体，对应表 t_kb_document。
 *
 * <p>定位：文档的权威元数据，保存在 MySQL。分块正文见 {@link KbChunk}，
 * 向量见 PostgreSQL 的 kb_chunk_vector（可随时重建）。</p>
 *
 * <p>表结构见 backend/src/main/resources/db/house_design.sql。</p>
 */
@Data
@TableName("t_kb_document")
public class KbDocument {

    /** 文档 ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档标题，用于列表展示与引用溯源 */
    private String title;

    /** 来源类型：文件 / URL / 纯文本，决定入库时的解析方式 */
    private KbSourceType sourceType;

    /** 原始文件名；URL 或纯文本来源可为空 */
    private String fileName;

    /** 文件扩展名（pdf / docx / md 等），用于选择解析器 */
    private String fileExt;

    /** 原文件在阿里云 OSS 的公网地址；非文件来源可为空 */
    private String ossUrl;

    /** 文件 SHA-256（十六进制），用于重复上传检测 */
    private String checksum;

    /** 可见范围：公开知识 / 用户私有资料，检索时必须在 SQL 层过滤 */
    private KbVisibility visibility;

    /** 私有文档归属用户（逻辑外键 → t_user.id）；公开文档为空 */
    private Long ownerUserId;

    /** 入库状态机：PENDING / PARSING / INDEXING / READY / FAILED */
    private KbDocumentStatus status;

    /** 入库失败原因，仅在 FAILED 状态下有值 */
    private String errorMessage;

    /**
     * 文档版本号，从 1 开始。
     * 重建索引时 +1，旧版本分块置为 inactive。
     * 注意：这是业务版本，不是 MyBatis-Plus 的乐观锁字段，不要加 @Version。
     */
    private Integer version;

    /** 当前有效版本的分块数量，用于列表展示与入库结果校验 */
    private Integer chunkCount;

    /** 创建时间（数据库默认 CURRENT_TIMESTAMP） */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间（数据库 ON UPDATE CURRENT_TIMESTAMP），也用于入库超时对账 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
