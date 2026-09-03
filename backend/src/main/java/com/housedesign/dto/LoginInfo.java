package com.housedesign.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功后返回给前端的信息：用户 ID + JWT 令牌。
 * 不含密码等敏感字段。
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LoginInfo {
    private Long id;
    private String token;
}
