package com.housedesign.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
/**
 * 注册请求体：用户名 + 密码 + 验证码 + 联系标识（手机号 / 邮箱二选一，恰好传一个）。
 * 二选一为 XOR 规则，注解无法表达，由 LoginService.register 校验。
 */
public class RegisterRequest {
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度3-32")
    private String username;

    // 手机注册时必填；@Pattern 对 null 放行，邮箱注册场景由 Service 层 XOR 校验拦截
    @Pattern(message = "手机号错误", regexp = "^1[3-9]\\d{9}$")
    private String phone;

    // 邮箱注册时必填（A-11）；手机注册场景为 null
    @Size(max = 128, message = "邮箱长度不能超过128")
    @Pattern(regexp = EMAIL_REGEX, message = "邮箱格式错误")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度8-32")
    private String password;
}
