package com.housedesign.Service.AI;

import java.util.List;

import com.housedesign.dto.response.AiChatMemoryResponse;

import dev.langchain4j.data.message.ImageContent;

public interface AiChatService {
    // 统一对话入口：纯文本 / 图文 / 纯图一条路径，手动管理会话记忆（成功才落库）
    AiChatMemoryResponse chat(String conversationId, String question, List<ImageContent> imageContents);

}
