package com.housedesign.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
/**
 * 注册请求体：用户名 + 手机号 + 短信验证码 + 密码（统一注册入口）
 */
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度3-32")
    private String username;

    @NotBlank(message = "手机号不能为空")
    @Pattern(message = "手机号错误", regexp = "^1[3-9]\\d{9}$")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度8-32")
    private String password;
}
