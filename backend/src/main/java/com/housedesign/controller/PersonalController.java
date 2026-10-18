package com.housedesign.controller;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.housedesign.Service.PersonalService;
import com.housedesign.common.EmailBindService;
import com.housedesign.common.PasswordChangeService;
import com.housedesign.common.Result;
import com.housedesign.dto.request.EmailBindCodeRequest;
import com.housedesign.dto.request.EmailBindRequest;
import com.housedesign.dto.request.PasswordChangeRequest;
import com.housedesign.dto.request.PasswordCodeRequest;
import com.housedesign.dto.request.UpdateRequest;
import com.housedesign.dto.response.PersonalDataResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RequestMapping("/api/auth")
@RequiredArgsConstructor
@RestController

// 显示个人信息
public class PersonalController {
    private final PersonalService personalService;
    private final PasswordChangeService passwordChangeService;
    private final EmailBindService emailBindService;
    // private PersonalData personalData; 并发隐患

    @GetMapping("/me")
    public Result<PersonalDataResponse> searchAll() {
        // 调用Service查询
        // 查询回显个人信息
        return Result.success(personalService.searchById());
    }

    @PutMapping("/me")
    public Result<PersonalDataResponse> updatePersonalData(@RequestBody @Valid UpdateRequest updateRequest) {
        // 调用Service更新字段并返回
        return Result.success(personalService.updatePersonalData(updateRequest));
    }

    // 发送修改密码验证码（A-5，需登录；收件地址后端按当前用户绑定信息决定）
    @PostMapping("/password/code")
    public Result<String> sendPasswordCode(@RequestBody @Valid PasswordCodeRequest request) {
        passwordChangeService.sendChangeCode(request.getChannel());
        return Result.success();
    }

    // 修改密码（A-5，需登录；成功后当前 token 拉黑，前端须重新登录）
    @PostMapping("/password/change")
    public Result<String> changePassword(@RequestBody @Valid PasswordChangeRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        passwordChangeService.changePassword(request.getChannel(), request.getCode(), request.getNewPassword(),
                token);
        return Result.success("密码修改成功，请重新登录");
    }

    // 发送绑定/换绑邮箱验证码（§10.8，需登录；新邮箱必须未被占用）
    @PostMapping("/bind/email/code")
    public Result<String> sendEmailBindCode(@RequestBody @Valid EmailBindCodeRequest request) {
        emailBindService.sendBindCode(request.getEmail());
        return Result.success();
    }

    // 确认绑定/换绑邮箱（§10.8，需登录；成功后 /me 掩码刷新，不影响登录态）
    @PostMapping("/bind/email")
    public Result<String> bindEmail(@RequestBody @Valid EmailBindRequest request) {
        return Result.success(emailBindService.bindEmail(request.getEmail(), request.getCode()));
    }

}
