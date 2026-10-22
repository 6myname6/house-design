package com.housedesign.Service;

import com.housedesign.dto.request.EmailLoginRequest;
import com.housedesign.dto.request.LoginRequest;
import com.housedesign.dto.request.RegisterRequest;
import com.housedesign.dto.response.LoginInfoResponse;

public interface LoginService {

    /** 统一注册：用户名 + 邮箱 + 邮件验证码 + 密码 */
    Long register(RegisterRequest registerRequest);

    // 账号密码登录
    LoginInfoResponse loginByPassword(LoginRequest loginRequest, String ip);

    // 邮箱验证码登录（邮箱必须已注册，A-11）
    LoginInfoResponse loginByEmail(EmailLoginRequest emailLoginRequest);
}
