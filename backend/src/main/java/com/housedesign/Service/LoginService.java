package com.housedesign.Service;

import com.housedesign.dto.LoginInfo;
import com.housedesign.dto.LoginRequest;
import com.housedesign.dto.RegisterRequest;

public interface LoginService {

    /** 注册 */
    Long register(RegisterRequest registerRequest);

    // 登录接口
    LoginInfo login(LoginRequest loginRequest);
}