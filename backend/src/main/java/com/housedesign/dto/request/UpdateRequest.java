package com.housedesign.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新请求体（patch 语义：字段为 null 表示本次不修改，@Size/@Pattern 对 null 均不生效）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRequest {
    @Size(max = 64, message = "昵称最长64个字符")
    private String nickname;

    @Size(max = 255, message = "头像地址过长")
    @Pattern(regexp = "^https?://.+", message = "头像地址不合法")
    private String avatar;
}
