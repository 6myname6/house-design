package com.housedesign.Service.AI.impl;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.exception.*;

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
            return aiChatAssistant.chat(question, imageContents);
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
