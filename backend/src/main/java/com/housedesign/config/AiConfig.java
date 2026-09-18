package com.housedesign.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

@Configuration
public class AiConfig {
    @Value("${langchain4j.open-ai.chat-model.api-key}")
    String apiKey;
    @Value("${langchain4j.open-ai.chat-model.base-url}")
    String baseUrl;
    @Value("${langchain4j.open-ai.chat-model.model-name}")
    String modelName;

    @Bean("visionChatModel")
    public ChatModel visionChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .logRequests(false)
                .logResponses(true)
                .build();
    }
}
