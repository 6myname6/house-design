package com.housedesign.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;

/**
 * 设计风格枚举：单一数据源，承担三重职责。
 *
 * 1. 入参校验：前端传 code，后端 fromCode 反查，非法回退 MODERN_MINIMALIST
 * 2. 响应展示：label 字段用于 ProjectResponse.styleLabel 给前端展示
 * 3. AI 生成提示：prompt 字段拼入智谱生成请求，作为风格化提示词
 *
 * 序列化控制：@JsonFormat(shape=OBJECT) 让枚举序列化为 {code, label} 对象，
 * 
 * @JsonIgnore 屏蔽 prompt，避免提示词泄露到前端。
 */
@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum DesignStyle {

    MODERN_MINIMALIST(
            "modern-minimalist",
            "现代简约",
            "中性灰白、干净直线、哑光材质、隐形把手、无主灯设计"),
    CREAM_FRENCH(
            "cream-french",
            "奶油轻法式",
            "暖象牙米色、柔布艺、拱形门洞、复古金点缀、石膏线"),
    ITALIAN_LUXURY(
            "italian-luxury",
            "意式轻奢",
            "高级灰、深木饰面、大理石、香槟金金属、皮革质感"),
    NEW_CHINESE(
            "new-chinese",
            "新中式",
            "胡桃木、墨灰、青瓷、黄铜、留白意境、对称布局"),
    LOG_WOOD(
            "log-wood",
            "原木风",
            "橡木、米麻织物、绿植点缀、温馨明亮、自然肌理");

    /** 前端传入的 code */
    private final String code;

    /** 响应返回的中文标签 */
    private final String label;

    /** 喂给 AI 的风格化提示词（@JsonIgnore 不参与序列化） */
    @JsonIgnore
    private final String prompt;

    DesignStyle(String code, String label, String prompt) {
        this.code = code;
        this.label = label;
        this.prompt = prompt;
    }

    /**
     * 按 code 反查枚举。
     * 
     * @return 命中返回对应枚举；null/空串/未命中返回 null，由调用方决定如何报错。
     */
    public static DesignStyle fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (DesignStyle s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }
}
