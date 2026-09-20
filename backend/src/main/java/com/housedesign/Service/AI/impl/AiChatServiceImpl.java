package com.housedesign.Service.AI.impl;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.Content;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.housedesign.Service.AI.AiChatAssistant;
import com.housedesign.Service.AI.AiChatService;
import com.housedesign.common.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {
    private final AiChatAssistant aiChatAssistant;
    private final ChatModel chatModel;

    // 纯文本对话
    @Override
    public String chat(String question) {
        try {
            return aiChatAssistant.chat(question);
        } catch (RateLimitException | TimeoutException | InternalServerException | UnresolvedModelServerException e) {
            throw new BusinessException(503, "对不起，当前AI服务繁忙，请稍后再试~");
        }
    }

    // 图文对话
    @Override
    public String chat(String question, List<ImageContent> imageContents) {
        try {
            SystemMessage systemMessage = SystemMessage.from(AiChatAssistant.SYSTEM_PROMPT);
            List<Content> contents = new ArrayList<>();
            contents.add(TextContent.from(question));
            contents.addAll(imageContents);
            UserMessage userMessage = UserMessage.from(contents);
            ChatResponse response = chatModel.chat(systemMessage, userMessage);
            return response.aiMessage().text();
        } catch (RateLimitException | TimeoutException | InternalServerException | UnresolvedModelServerException e) {
            throw new BusinessException(503, "对不起，当前AI服务繁忙，请稍后再试~");
        }
    }

    // 图片理解
    @Override
    public String chat(List<ImageContent> imageContents) {

        return this.chat("请解释这张图", imageContents);
    }

}
