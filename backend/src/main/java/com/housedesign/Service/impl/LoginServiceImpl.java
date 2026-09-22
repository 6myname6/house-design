package com.housedesign.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.housedesign.Service.LoginService;
import com.housedesign.common.BusinessException;
import com.housedesign.common.JwtUtil;
import com.housedesign.common.LoginRateLimiter;
import com.housedesign.common.SmsCodeService;
import com.housedesign.dto.request.LoginRequest;
import com.housedesign.dto.request.RegisterRequest;
import com.housedesign.dto.request.SmsLoginRequest;
import com.housedesign.dto.response.LoginInfoResponse;
import com.housedesign.entity.User;
import com.housedesign.mapper.UserMapper;

import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class LoginServiceImpl implements LoginService {
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginRateLimiter loginRateLimiter;
    private final SmsCodeService smsCodeService;

    @Override
    public Long register(RegisterRequest registerRequest) {
        // 检查是否有重复用户名
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, registerRequest.getUsername()));
        if (count > 0) {
            // 有重复用户名则返回空
            return null;
        }
        // 没有重复用户名则成功注册：new一个新用户并插入数据库
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setNickname("筑梦家er_" + cn.hutool.core.util.RandomUtil.randomString(6));
        // 密码加密存储
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        try {
            userMapper.insert(user);

        } catch (DuplicateKeyException e) {
            log.warn("用户名 {} 并发注册冲突", registerRequest.getUsername());
            return null;
        }
        return user.getId();
    }

    @Override
    public LoginInfoResponse login(LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        // 锁定期检查：已达失败上限则直接抛出429
        loginRateLimiter.checkLocked(username);
        // 1.根据用户名查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, loginRequest.getUsername()));
        if (user == null) {
            loginRateLimiter.recordFailure(username);
            log.warn("用户名不存在！");
            return null;
        }
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            // 密码错误，计数器+1
            loginRateLimiter.recordFailure(username);
            return null;
        }
        // 用户名存在，密码正确，清除计数器，返回登录信息
        // 生成 JWT 令牌
        loginRateLimiter.clearOnSuccess(username);
        String token;
        token = jwtUtil.createToken(user.getId(), user.getUsername());
        return new LoginInfoResponse(user.getId(), token);
    }

    @Override
    public LoginInfoResponse loginByPhone(SmsLoginRequest smsLoginRequest) {
        String phone = smsLoginRequest.getPhone();
        String code = smsLoginRequest.getCode();
        // 1.校验验证码
        if (!smsCodeService.verify(phone, code)) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        // 2.按手机号查用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));

        // 3.不存在则自动建档
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setUsername("用户" + RandomUtil.randomString(6));
            user.setNickname("筑梦家er" + RandomUtil.randomString(6));
            userMapper.insert(user);
        }
        String token = jwtUtil.createToken(user.getId(), user.getUsername());
        return new LoginInfoResponse(user.getId(), token);
    }

}
