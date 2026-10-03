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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
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
    @Value("${app.storage.public-base-url}")
    private String storagePublicBaseUrl;
    @Value("${app.storage.location:./storage}")
    private String storageLocation;
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
     * 白名单策略（SSRF 防护）：只接受本系统两个存储基址——
     * ① OSS 公网地址：智谱云端可自行下载，直接传 URI；
     * ② 本地存储地址（localhost）：云端访问不到用户本机，读盘转 base64；
     * 其他域名一律拒绝，防止借模型服务器探测任意外网/内网地址。
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
        // 3.本地存储地址：剥前缀 -> 定位磁盘文件 -> 穿越校验 -> 读盘转 base64
        if (url.startsWith(storagePublicBaseUrl)) {
            String relative = url.substring(storagePublicBaseUrl.length()).replaceFirst("^/", "");
            Path base = Paths.get(storageLocation).toAbsolutePath().normalize();
            Path file = base.resolve(relative).normalize();
            // normalize 后必须仍在存储根目录内，防止 ../../ 路径穿越
            if (!file.startsWith(base)) {
                throw new BusinessException(400, "非法的图片地址");
            }
            try {
                byte[] bytes = Files.readAllBytes(file);
                String base64 = Base64.getEncoder().encodeToString(bytes);
                return ImageContent.from(base64, mimeFromUrl(url));
            } catch (IOException e) {
                log.warn("AI 对话读取本地图片失败：{}", file, e);
                throw new BusinessException(400, "图片读取失败，请重新上传");
            }
        }
        // 4.两个可信基址都不匹配：拒绝
        throw new BusinessException(400, "仅允许使用已上传的图片地址");
    }

    /** 从图片 URL 的扩展名推断 MIME；URL 可能带查询参数，先用 URI 取纯路径 */
    private String mimeFromUrl(String url) {
        String path;
        try {
            path = URI.create(url).getPath();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "非法的图片地址");
        }
        if (path == null) {
            throw new BusinessException(400, "非法的图片地址");
        }
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        // 走到这里说明图片不是上传白名单格式，理论上不会发生（上传时已拦）
        throw new BusinessException(400, "仅支持 jpg/png/gif/webp 格式图片");
    }

}
