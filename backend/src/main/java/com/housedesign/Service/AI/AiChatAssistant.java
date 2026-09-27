package com.housedesign.Service.AI;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService(chatMemoryProvider = "chatMemoryProvider")
public interface AiChatAssistant {
    String SYSTEM_PROMPT = "你是一个资深的住宅装修设计师，了解各式各样的装修风格，对建材行业了如指掌，能回答用户提出的各种问题。非相关问题不予回答，礼貌拒绝";

    // 纯文本对话
    @SystemMessage(SYSTEM_PROMPT)
    String chat(String question);

    // 记忆上下文（@SystemMessage 必须在方法上：1.0.1 的 DefaultAiServices 只读取方法级注解）
    @SystemMessage(SYSTEM_PROMPT)
    String chat(@MemoryId String conversationId, @UserMessage String question);

}
