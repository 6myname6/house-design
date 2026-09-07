package com.housedesign.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.housedesign.Service.PersonalService;
import com.housedesign.common.Result;
import com.housedesign.dto.request.UpdateRequest;
import com.housedesign.dto.response.PersonalDataResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RequestMapping("/api/auth")
@RequiredArgsConstructor
@RestController

// 显示个人信息
public class PersonalController {
    private final PersonalService personalService;
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

}
