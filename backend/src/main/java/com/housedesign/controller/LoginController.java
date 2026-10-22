package com.housedesign.controller;

import com.housedesign.Service.LoginService;
import com.housedesign.Utils.IpUtils;
import com.housedesign.Utils.JwtUtil;
import com.housedesign.Utils.Result;
import com.housedesign.annotation.NoRepeatSubmit;
import com.housedesign.common.ActiveTokenService;
import com.housedesign.common.EmailCodeService;
import com.housedesign.common.TokenBlackListService;
import com.housedesign.dto.request.EmailCodeRequest;
import com.housedesign.dto.request.EmailLoginRequest;
import com.housedesign.dto.request.LoginRequest;
import com.housedesign.dto.request.RegisterRequest;
import com.housedesign.dto.response.LoginInfoResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/api/auth")
@RestController
@Slf4j
@RequiredArgsConstructor
public class LoginController {
    private final TokenBlackListService tokenBlackListService;
    private final EmailCodeService emailCodeService;
    private final LoginService userService;
    private final JwtUtil jwtUtil;
    private final ActiveTokenService activeTokenService;

    // 登录接口
    @PostMapping("/login")
    // 1. 获取前端传入参数：用户名，密码
    public Result<String> login(@RequestBody @Valid LoginRequest loginRequest,
            HttpServletRequest httpServletRequest) {
        log.info("用户：{}登录中...", loginRequest.getUsername());
        // 取Ip
        String ip = IpUtils.getClientIp(httpServletRequest);
        // 2.调用UserService.login方法登录用户
        LoginInfoResponse info = userService.loginByPassword(loginRequest, ip);
        // 3.根据登录结果返回响应
        return Result.success(info.getToken());
    }

    @NoRepeatSubmit
    @PostMapping("/register")
    // 1. 获取前端传入参数：用户名，密码
    public Result<Long> register(@RequestBody @Valid RegisterRequest registerRequest) {
        log.info("注册：username={}", registerRequest.getUsername());
        // 2.调用UserService.register方法注册用户
        return Result.success(userService.register(registerRequest));
    }

    // 发送邮箱验证码（A-11）
    @PostMapping("/email/code")
    public Result<String> postEmailCode(@RequestBody @Valid EmailCodeRequest emailCodeRequest,
            HttpServletRequest httpServletRequest) {
        String ip = IpUtils.getClientIp(httpServletRequest);
        emailCodeService.sendCode(emailCodeRequest.getEmail(), ip, emailCodeRequest.getScene());
        return Result.success();
    }

    // 邮箱验证码登录（邮箱必须已注册，A-11）
    @PostMapping("/email/login")
    public Result<String> emailCodeLogin(@RequestBody @Valid EmailLoginRequest emailLoginRequest) {
        LoginInfoResponse infoResponse = userService.loginByEmail(emailLoginRequest);
        return Result.success(infoResponse.getToken());
    }

    // 登出接口
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        tokenBlackListService.blacklist(token);
        activeTokenService.delete(jwtUtil.getUserId(token));
        return Result.success("成功退出");
    }

}
