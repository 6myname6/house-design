package com.housedesign.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库检索响应体：KbSearchHit
 * 包含搜索召回的候选文档，经过转换后用于返回给上层调用方
 * 
 * @param chunkId       切片id
 * @param documentId    文档id
 * @param documentTitle 文档标题
 * @param heading       文档标题
 * @param document      内容
 * @param score         原始召回分数
 * @param rerankScore   Rerank相关度，0-1，未启用或降级时为null
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KbSearchHit {
    /** 切片id */
    private Long chunkId;

    private Long documentId;

    private String documentTitle;

    private String heading;

    private String content;

    private double score;
    /** Rerank相关度，0-1，未启用或降级时为null */
    private Double rerankScore;

}
