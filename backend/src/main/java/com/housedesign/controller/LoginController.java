package com.housedesign.controller;

import com.housedesign.Service.LoginService;
import com.housedesign.dto.LoginInfo;
import com.housedesign.dto.LoginRequest;
import com.housedesign.dto.RegisterRequest;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.housedesign.common.Result;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/api/auth")
@RestController
@Slf4j
@RequiredArgsConstructor
public class LoginController {
    // 登录
    private final LoginService userService;

    // 登录接口
    @PostMapping("/login")
    // 1. 获取前端传入参数：用户名，密码
    public Result<String> login(@RequestBody LoginRequest loginRequest) {
        log.info("登录：{}", loginRequest);
        // 2.调用UserService.login方法登录用户
        LoginInfo info = userService.login(loginRequest);
        // 3.根据登录结果返回响应
        if (info != null) {
            return Result.success(info.getToken());
        }
        return Result.error(400, "用户名或密码错误");
    }

    @PostMapping("/register")
    // 1. 获取前端传入参数：用户名，密码
    public Result<Long> register(@RequestBody @Valid RegisterRequest registerRequest) {
        log.info("注册：username={}", registerRequest.getUsername());
        // 2.调用UserService.register方法注册用户
        Long userId = userService.register(registerRequest);
        // 3.根据注册结果返回响应
        if (userId != null) {
            return Result.success(userId);
        }
        return Result.error(400, "抱歉，这个用户名被别人使用了，换一个试试吧~");
    }

}
