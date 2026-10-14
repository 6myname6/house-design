package com.housedesign.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.housedesign.Service.AI.AiChatService;
import com.housedesign.common.BusinessException;
import com.housedesign.common.Result;
import com.housedesign.dto.request.AiChatRequest;

import dev.langchain4j.data.message.ImageContent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RequestMapping("/api/ai")
@RestController
@RequiredArgsConstructor
@Slf4j
public class AiChatController {
    private final AiChatService aiChatService;

    // 本系统图片唯一可信基址（OSS 公网域名），用于多模态图片 URL 白名单（SSRF 防护）
    @Value("${app.storage.oss.public-base-url}")
    private String ossPublicBaseUrl;

    // AI对话

    @PostMapping("/chat")
    // 纯文本返回 AiChatMemoryResponse，图文/纯图返回 String，故用通配符兼容两种形态
    public Result<?> aiChat(@RequestBody @Valid AiChatRequest aiChatRequest) {

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

    /**
     * 把已上传图片的可信 URL 转成多模态消息。
     *
     * 白名单策略（SSRF 防护）：图片已统一持久化到 OSS，只接受本系统 OSS 公网基址，
     * 由模型厂商服务器自行下载；其他域名一律拒绝，防止借模型服务器探测任意外网/内网地址。
     */
    private ImageContent toImageContent(String url) {
        // 1.空值防御
        if (url == null || url.isBlank()) {
            throw new BusinessException(400, "图片地址不能为空");
        }
        // 2.OSS 公网地址：直接交给模型，由模型厂商服务器自行下载
        if (url.startsWith(ossPublicBaseUrl)) {
            return ImageContent.from(URI.create(url));
        }
        // 3.非可信基址：拒绝
        throw new BusinessException(400, "仅允许使用已上传的图片地址");
    }

}
