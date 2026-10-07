package com.housedesign.controller;

import com.housedesign.Service.LoginService;
import com.housedesign.annotation.NoRepeatSubmit;
import com.housedesign.common.TokenBlackListService;
import com.housedesign.dto.request.LoginRequest;
import com.housedesign.dto.request.RegisterRequest;
import com.housedesign.dto.request.SmsCodeRequest;
import com.housedesign.dto.request.SmsLoginRequest;
import com.housedesign.dto.response.LoginInfoResponse;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.housedesign.common.Result;
import com.housedesign.common.SmsCodeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/api/auth")
@RestController
@Slf4j
@RequiredArgsConstructor
public class LoginController {
    private final TokenBlackListService tokenBlackListService;
    private final SmsCodeService smsCodeService;
    private final LoginService userService;

    // 登录接口
    @PostMapping("/login")
    // 1. 获取前端传入参数：用户名，密码
    public Result<String> login(@RequestBody @Valid LoginRequest loginRequest) {
        log.info("用户：{}登录中...", loginRequest.getUsername());
        // 2.调用UserService.login方法登录用户
        LoginInfoResponse info = userService.login(loginRequest);
        // 3.根据登录结果返回响应
        if (info != null) {
            return Result.success(info.getToken());
        }
        return Result.error(400, "用户名或密码错误");
    }

    @NoRepeatSubmit
    @PostMapping("/register")
    // 1. 获取前端传入参数：用户名，密码
    public Result<Long> register(@RequestBody @Valid RegisterRequest registerRequest) {
        log.info("注册：username={}", registerRequest.getUsername());
        // 2.调用UserService.register方法注册用户
        return Result.success(userService.register(registerRequest));
    }

    // 发送验证码
    @PostMapping("/sms/code")
    public Result<String> postAcode(@RequestBody @Valid SmsCodeRequest smsCodeRequest) {
        smsCodeService.sendCode(smsCodeRequest.getPhone());
        return Result.success();
    }

    // 验证码登录或注册
    @PostMapping("/sms/login")
    public Result<String> smsCodeLoginOrRegister(@RequestBody @Valid SmsLoginRequest smsLoginRequest) {
        LoginInfoResponse infoResponse = userService.loginByPhone(smsLoginRequest);
        return Result.success(infoResponse.getToken());
    }

    // 登出接口
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        tokenBlackListService.blacklist(token);
        return Result.success("成功退出");
    }

}
