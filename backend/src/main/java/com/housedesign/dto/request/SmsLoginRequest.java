package com.housedesign.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsLoginRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(message = "手机号错误", regexp = "^1[3-9]\\d{9}$")
    private String phone;
    @NotBlank(message = "验证码不能为空")
    private String code;
}
