package com.housedesign.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailCodeRequest {
    // 简化 RFC 邮箱正则；与需求文档 §10.6 冻结规则一致
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    @NotBlank(message = "邮箱不能为空")
    @Size(max = 128, message = "邮箱长度不能超过128")
    @Pattern(regexp = EMAIL_REGEX, message = "邮箱格式错误")
    private String email;

    /** 发码场景：login=邮箱登录（邮箱必须已注册），register=注册（邮箱必须未注册） */
    @NotBlank(message = "场景不能为空")
    @Pattern(regexp = "^(login|register)$", message = "非法场景")
    private String scene;
}
