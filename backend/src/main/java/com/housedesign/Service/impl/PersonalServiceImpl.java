package com.housedesign.Service.impl;

import org.springframework.stereotype.Service;

import com.housedesign.Service.PersonalService;
import com.housedesign.common.UserContext;
import com.housedesign.dto.request.UpdateRequest;
import com.housedesign.dto.response.PersonalDataResponse;
import com.housedesign.entity.User;
import com.housedesign.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PersonalServiceImpl implements PersonalService {
    private final UserMapper userMapper;

    @Override
    // 根据id查询个人信息
    public PersonalDataResponse searchById() {
        // 1.当前用户 ID 由 JwtInterceptor 解析 token 后放入，无需传参
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);
        // 2. 将个人信息写入personalData
        PersonalDataResponse personalData = new PersonalDataResponse();
        personalData.setId(user.getId());
        personalData.setUsername(user.getUsername());
        personalData.setAvatar(user.getAvatar());
        return personalData;
    }

    @Override
    public PersonalDataResponse updatePersonalData(UpdateRequest updateRequest) {
        // 1.当前用户 ID 由 JwtInterceptor 解析 token 后放入，无需传参
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);
        // 2.修改用户信息
        user.setAvatar(updateRequest.getAvatar());
        user.setNickname(updateRequest.getNickname());
        // 3.更新数据库信息
        userMapper.updateById(user);
        // 4.PersonalData回显
        PersonalDataResponse personalData = new PersonalDataResponse();
        personalData.setNickname(user.getNickname());
        personalData.setId(user.getId());
        personalData.setUsername(user.getUsername());
        personalData.setAvatar(user.getAvatar());
        return personalData;
    }

}
