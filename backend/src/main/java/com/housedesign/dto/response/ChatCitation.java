package com.housedesign.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话引用：本次回答所依据的知识库分块，供前端定位原文。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatCitation {
    private Long documentId;
    private String title;
    private Long chunkId;
    private String heading;
    private String snippet;// 片段正文
}
