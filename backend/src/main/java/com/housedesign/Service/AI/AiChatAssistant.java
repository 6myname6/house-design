package com.housedesign.Service.AI;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService(chatMemoryProvider = "chatMemoryProvider")
@SystemMessage(AiChatAssistant.SYSTEM_PROMPT)
public interface AiChatAssistant {
    String SYSTEM_PROMPT = "你是一个资深的住宅装修设计师，了解各式各样的装修风格，对建材行业了如指掌，能回答用户提出的各种问题。非相关问题不予回答，礼貌拒绝";

    // 纯文本对话
    String chat(String question);

    // 记忆上下文
    String chat(@MemoryId String conversationId, @UserMessage String question);

}
