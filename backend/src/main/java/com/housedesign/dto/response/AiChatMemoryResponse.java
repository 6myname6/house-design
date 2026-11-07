package com.housedesign.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 对话响应体：会话标识、回答正文与本次引用的知识库分块。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatMemoryResponse {
    // 会话标识
    private String conversationId;
    // AI回复
    private String answer;
    // 本次回答引用的知识库分块；未命中时为空数组
    private List<ChatCitation> citations;
}
