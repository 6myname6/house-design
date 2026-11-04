package com.housedesign.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库检索响应体：KbSearchHit
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
}
