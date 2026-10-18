package com.housedesign.Service.impl;

import org.springframework.stereotype.Service;

import com.housedesign.Service.PersonalService;
import com.housedesign.Util.UserContext;
import com.housedesign.common.BusinessException;
import com.housedesign.dto.request.UpdateRequest;
import com.housedesign.dto.response.PersonalDataResponse;
import com.housedesign.entity.User;
import com.housedesign.mapper.UserMapper;

import cn.hutool.core.util.StrUtil;
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
        return toResponse(user);
    }

    @Override
    public PersonalDataResponse updatePersonalData(UpdateRequest updateRequest) {
        // 1.当前用户 ID 由 JwtInterceptor 解析 token 后放入，无需传参
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);
        // 2.修改用户信息
        if ((updateRequest.getAvatar() == null
                || updateRequest.getAvatar().isBlank())
                && (updateRequest.getNickname() == null
                        || updateRequest.getNickname().isBlank())) {
            throw new BusinessException(400, "更改不能为空");
        }
        if (!(updateRequest.getAvatar() == null || updateRequest.getAvatar().isBlank())) {
            user.setAvatar(updateRequest.getAvatar());
        }
        if (!(updateRequest.getNickname() == null || updateRequest.getNickname().isBlank())) {
            user.setNickname(updateRequest.getNickname());
        }

        // 3.更新数据库信息
        userMapper.updateById(user);
        // 4.PersonalData回显
        return toResponse(user);
    }

    // 实体 → 响应 DTO（含手机/邮箱掩码，供前端判断可用的改密验证通道）
    private PersonalDataResponse toResponse(User user) {
        PersonalDataResponse personalData = new PersonalDataResponse();
        personalData.setId(user.getId());
        personalData.setUsername(user.getUsername());
        personalData.setNickname(user.getNickname());
        personalData.setAvatar(user.getAvatar());
        personalData.setMaskedPhone(maskPhone(user.getPhone()));
        personalData.setMaskedEmail(maskEmail(user.getEmail()));
        return personalData;
    }

    // 13800138000 → 138****8000；为空返回 null
    private String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 7) {
            return null;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    // zhangsan@qq.com → z***@qq.com；为空返回 null
    private String maskEmail(String email) {
        if (StrUtil.isBlank(email) || email.indexOf('@') <= 0) {
            return null;
        }
        return email.charAt(0) + "***" + email.substring(email.indexOf('@'));
    }

}
