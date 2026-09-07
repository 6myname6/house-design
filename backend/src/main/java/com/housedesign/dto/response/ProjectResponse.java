package com.housedesign.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设计项目响应体。
 * 不含 userId、designImagePath 等内部字段，避免泄露用户标识与存储路径。
 */
@Data
public class ProjectResponse {

    /** 项目 ID */
    private Long id;

    /** 项目名称 */
    private String name;

    /** 项目描述（可为空） */
    private String description;

    /** 装修风格 code（如 modern-minimalist） */
    private String style;

    /** 装修风格中文名（由 Service 层根据 style 解析填充，如「现代简约」） */
    private String styleLabel;

    /** 设计图可访问 URL */
    private String designImageUrl;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
