package com.housedesign.Utils;

import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

/**
 * Markdown 文本分块器（纯函数，无外部依赖，方便单测）。
 *
 * <p>
 * 职责：把一篇文档正文切成一批「章节标题 + 正文」的小块，供入库写入 t_kb_chunk。
 * </p>
 *
 * <p>
 * 规则：先按 Markdown 标题分段；段落不超过 maxChars 就直接成块；
 * 超长段落交给 LangChain4j 的递归分块器，按 段落→行→句子→词→字符 逐级细切。
 * </p>
 */
public class KbTextChunker {
    /** 工具类不允许实例化 */
    private KbTextChunker() {
    }

    // ==================== 私有方法 ====================

    /**
     * 把一个段落的正文收集成 1 个或多个分块。
     *
     * @param out          结果收集器，输出顺序与输入顺序一致
     * @param heading      当前段落所属章节标题；正文第一个标题之前的部分为 null
     * @param body         当前段落的正文缓冲
     * @param maxChars     单块最大字符数
     * @param overlapChars 超长段落切分时相邻块的重叠字符数，必须小于 maxChars
     */
    private static void collect(List<ChunkDraft> out, String heading,
            StringBuilder body, int maxChars,
            int overlapChars) {
        // 取出正文并去掉首尾空白；空段落（如连续两个标题之间没有正文）直接丢弃
        String content = body.toString().strip();
        if (content.isEmpty()) {
            return;
        }
        // 短段落：整段作为一个块，不必进 LangChain4j
        if (content.length() <= maxChars) {
            out.add(new ChunkDraft(heading, content));
            return;
        }
        // 超长段落：交给 LangChain4j 递归切分（按自然边界逐级细切）
        List<TextSegment> segments = DocumentSplitters.recursive(maxChars, overlapChars)
                .split(Document.from(content));
        for (TextSegment segment : segments) {
            String piece = segment.text().strip();
            if (!piece.isEmpty()) {
                out.add(new ChunkDraft(heading, piece));
            }
        }
    }

    /**
     * 分块草稿：尚未落库的分块内容。
     *
     * @param heading 章节标题；正文第一个标题之前的部分为 null
     * @param content 分块正文，已去除首尾空白
     */
    public record ChunkDraft(String heading, String content) {
    }

    /**
     * 把整篇文本切成有序的分块列表。
     *
     * @param text         原始正文（通常是 t_kb_document.raw_content）
     * @param maxChars     单块最大字符数
     * @param overlapChars 超长段落相邻块的重叠字符数
     * @return 分块草稿，顺序与原文一致；空文本返回空列表
     */
    public static List<ChunkDraft> split(String text, int maxChars, int overlapChars) {
        List<ChunkDraft> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        // 统一换行符并去掉首尾空白，避免 Windows / Unix 换行差异影响分段
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n").strip();
        String heading = null; // 当前章节标题
        StringBuilder body = new StringBuilder(); // 当前章节正文缓冲
        for (String line : normalized.split("\n", -1)) {
            // 命中 Markdown 标题行：先把上一段收口，再记录新标题
            if (line.stripLeading().startsWith("#")) {
                collect(result, heading, body, maxChars, overlapChars);
                heading = line.replaceAll("^#+\\s*", "").trim();
                body.setLength(0);
            } else {
                // 普通正文行：追加到当前段落
                body.append(line).append("\n");
            }
        }
        // 收口最后一个段落
        collect(result, heading, body, maxChars, overlapChars);
        return result;
    }

}
