package com.housedesign.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.housedesign.Service.AI.AiChatService;
import com.housedesign.common.BusinessException;
import com.housedesign.common.Result;
import com.housedesign.dto.request.AiChatRequest;

import dev.langchain4j.data.message.ImageContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RequestMapping("/api/ai")
@RestController
@RequiredArgsConstructor
@Slf4j
public class AiChatController {
    private final AiChatService aiChatService;

    // AI对话

    @PostMapping("/chat")
    // 纯文本返回 AiChatMemoryResponse，图文/纯图返回 String，故用通配符兼容两种形态
    public Result<?> aiChat(@RequestBody AiChatRequest aiChatRequest) {

        if ((aiChatRequest.getQuestion() == null || aiChatRequest.getQuestion().isBlank())
                && (aiChatRequest.getImages() == null || aiChatRequest.getImages().isEmpty())) {
            log.warn("输入为空");
            throw new BusinessException(400, "输入不能为空！");
        }

        // 只传文本
        if (aiChatRequest.getImages() == null || aiChatRequest.getImages().isEmpty()) {
            log.info("用户提问：{}", aiChatRequest.getQuestion());
            // 纯文本：透传会话 id，返回 {conversationId, answer}
            return Result.success(aiChatService.chat(
                    aiChatRequest.getConversationId(), aiChatRequest.getQuestion()));
        }

        // 获取ImageContents传参
        List<ImageContent> imageContents = aiChatRequest.getImages().stream()
                .map(this::toImageContent)
                .toList();

        // 只传图片
        if (aiChatRequest.getQuestion() == null || aiChatRequest.getQuestion().isBlank()) {
            log.info("用户发起图片提问");
            return Result.success(aiChatService.chat(imageContents));
        }
        log.info("用户提问：{}+图片", aiChatRequest.getQuestion());
        return Result.success(aiChatService.chat(aiChatRequest.getQuestion(), imageContents));
    }

    private ImageContent toImageContent(String url) {
        int comma = url == null ? -1 : url.indexOf(",");
        int semi = url == null ? -1 : url.indexOf(";");
        if (url == null || !url.startsWith("data:") || comma < 0 || semi < 5) {
            throw new BusinessException(400, "图片格式错误，请上传DataURL（base64）格式的图片");
        }
        String meta = url.substring(0, comma);
        String mimeType = meta.substring(5, semi);
        String base64 = url.substring(comma + 1);
        return ImageContent.from(base64, mimeType);
    }

}
