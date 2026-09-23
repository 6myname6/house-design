package com.housedesign.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

@Configuration
public class AiConfig {
    // 将持有所有的会话记忆：conversationId -> 该会话的记忆
    private final Map<String, ChatMemory> memories = new ConcurrentHashMap<>();

    @Bean
    ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> memories.computeIfAbsent((String) memoryId,
                id -> MessageWindowChatMemory.builder()
                        .id(id)
                        .maxMessages(10)
                        .build());
    }
}
