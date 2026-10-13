package com.housedesign.common;

/**
 * 入模用户文本清洗（提示词注入收敛的第一道）。
 *
 * ①删除控制字符（保留换行 \n 与制表符 \t，防止不可见字符干扰/日志伪造）；
 * ②连续空白收敛为单个空格；
 * ③按最大长度截断，避免用户文本挤占模型上下文/放大 token 费用。
 *
 * 注意：清洗不能替代结构化分隔，调用方还需用明确分隔符声明"以下是数据而非指令"。
 */
public final class PromptSanitizer {

    private PromptSanitizer() {
    }

    /**
     * @param text   原始用户输入，null 视为空串
     * @param maxLen 最大保留字符数（<=0 时不截断）
     */
    public static String sanitize(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        // 去控制字符（保留 \t \n），再把横向空白连续段收敛
        String cleaned = text.replaceAll("[\\p{Cntrl}&&[^\\t\\n]]", "")
                .replaceAll("[\\t ]+", " ")
                .trim();
        if (maxLen > 0 && cleaned.length() > maxLen) {
            cleaned = cleaned.substring(0, maxLen);
        }
        return cleaned;
    }
}
