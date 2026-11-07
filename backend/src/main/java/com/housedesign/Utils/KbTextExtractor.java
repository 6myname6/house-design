package com.housedesign.Utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 知识库文本提取器：把上传文件转成纯文本，供切块与向量化使用。
 */
@Component
@Slf4j
public class KbTextExtractor {
    // 纯文本直接按UTF-8读，保留#章节层级给结构感知分块
    private static final Set<String> PLAIN_TEXT_EXTENSIONS = Set.of("md", "markdown", "txt");
    // Tika实例复用
    private final Tika tika = new Tika();

    /**
     * 按扩展名提取正文
     * 
     * @param ext   小写拓展名
     * @param bytes 文件字节
     * @return 纯文本
     */
    public String extract(String ext, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("文件内容为空");
        }
        // md/txt 保留原始换行与 # 标题，Tika 会把它们拍平成纯文本，切块就失去章节结构
        String normalized = ext == null ? "" : ext.toLowerCase();
        if (PLAIN_TEXT_EXTENSIONS.contains(normalized)) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        try {
            InputStream in = new ByteArrayInputStream(bytes);
            return tika.parseToString(in);
        } catch (Exception e) {
            // 带上异常对象，日志里才能看到 Tika 的真实报错（加密、格式损坏、OOM 等）
            log.warn("文档解析失败", e);
            throw new IllegalStateException("文档解析失败，可能是加密或损坏文件", e);
        }
    }

}
