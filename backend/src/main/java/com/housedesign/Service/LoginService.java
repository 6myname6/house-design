package com.housedesign.Service;

import com.housedesign.dto.request.LoginRequest;
import com.housedesign.dto.request.RegisterRequest;
import com.housedesign.dto.response.LoginInfoResponse;

public interface LoginService {

    /** 注册 */
    Long register(RegisterRequest registerRequest);

    // 登录接口
    LoginInfoResponse login(LoginRequest loginRequest);
}