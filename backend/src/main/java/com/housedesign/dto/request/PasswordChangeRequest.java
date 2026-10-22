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
public class PasswordChangeRequest {
    /** 验证通道，须与发码时一致：固定 email */
    @NotBlank(message = "通道不能为空")
    @Pattern(regexp = "^email$", message = "非法通道")
    private String channel;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度8-32")
    private String newPassword;
}
