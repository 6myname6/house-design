package com.housedesign.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.housedesign.entity.KbDocumentStatus;
import com.housedesign.entity.KbSourceType;
import com.housedesign.entity.KbVisibility;

import lombok.Data;

/**
 * 知识库文档详情 / 列表项。
 *
 * <p>不返回 checksum 与 ossUrl 等内部字段：前者是去重用的实现细节，
 * 后者是存储路径，前端均无使用场景。</p>
 */
@Data
public class KbDocumentResponse {

    /** 文档 ID */
    private Long id;

    /** 文档标题 */
    private String title;

    /** 来源类型：FILE / URL / TEXT */
    private KbSourceType sourceType;

    /** 原始文件名（TEXT 录入时为空） */
    private String fileName;

    /** 可见性：PUBLIC / PRIVATE */
    private KbVisibility visibility;

    /** 入库状态 */
    private KbDocumentStatus status;

    /** 失败原因，仅 status = FAILED 时有值 */
    private String errorMessage;

    /** 当前文档版本 */
    private Integer version;

    /** 当前版本的分块数，READY 前为 0 */
    private Integer chunkCount;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
