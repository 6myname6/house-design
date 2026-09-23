package com.housedesign.Service.AI;

import java.util.List;

import com.housedesign.dto.response.AiChatMemoryResponse;

import dev.langchain4j.data.message.ImageContent;

public interface AiChatService {
    // 纯对话服务
    AiChatMemoryResponse chat(String conversationId, String question);

    // 对话+图片理解
    String chat(String question, List<ImageContent> imageContent);

    // 图片理解
    String chat(List<ImageContent> imageContent);

}
