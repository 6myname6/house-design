package com.housedesign.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatMemoryResponse {
    // 会话标识
    private String conversationId;
    // AI回复
    private String answer;
}
