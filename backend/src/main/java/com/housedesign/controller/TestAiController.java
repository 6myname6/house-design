package com.housedesign.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.housedesign.Service.AI.AiChatAssistant;
import com.housedesign.common.BusinessException;

import dev.langchain4j.data.message.ImageContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping
@Slf4j
@RequiredArgsConstructor
public class TestAiController {
    private final AiChatAssistant aiChatService;

    @GetMapping("/api/chat")
    public String askAi(@RequestParam(required = false, defaultValue = "null") String question,
            @RequestParam(required = false, defaultValue = "null") List<ImageContent> imageContents) {
        // 输入为空
        if ((question == null || question.isBlank()) && imageContents == null) {
            log.warn("输入为空！");
            throw new BusinessException(400, "输入不能为空");
        }
        // 只传文本
        if (imageContents == null) {
            return aiChatService.chat(question);
        }
        // 只传图片
        if (question == null || question.isBlank()) {
            return aiChatService.chat("请解释这张图", imageContents);
        }
        // 都传
        return aiChatService.chat(question, imageContents);
    }

}
