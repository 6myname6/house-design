package com.housedesign.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordCodeRequest {
    /** 验证通道：email=绑定邮箱（真实邮件）；收件地址后端按 userId 查库 */
    @NotBlank(message = "通道不能为空")
    @Pattern(regexp = "^email$", message = "非法通道")
    private String channel;
}
