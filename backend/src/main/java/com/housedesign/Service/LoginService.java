package com.housedesign.Service;

import com.housedesign.dto.request.LoginRequest;
import com.housedesign.dto.request.RegisterRequest;
import com.housedesign.dto.request.SmsLoginRequest;
import com.housedesign.dto.response.LoginInfoResponse;

public interface LoginService {

    /** 统一注册：用户名 + 手机号 + 验证码 + 密码 */
    Long register(RegisterRequest registerRequest);

    // 账号密码登录
    LoginInfoResponse login(LoginRequest loginRequest, String ip);

    // 手机号验证码登录（手机号必须已注册）
    LoginInfoResponse loginByPhone(SmsLoginRequest smsLoginRequest);
}