package com.housedesign.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
/**
 * 注册请求体
 */
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 32, message = "用户名长度1-32")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度8-32")
    private String password;
}
