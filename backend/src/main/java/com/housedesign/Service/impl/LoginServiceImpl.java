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
    /** 用户不存在时用于拉平响应耗时的假 BCrypt 哈希（cost=10），仅用于时序对齐 */
    private static final String DUMMY_HASH = "$2a$10$jOQld7IG2qbJtpYYxx1LF.wO0tAhn08mUYcPvkR7tj9nhS07Pm3ce";

    @Override
    public Long register(RegisterRequest registerRequest) {
        // 检查是否有重复用户名
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, registerRequest.getUsername()));
        if (count > 0) {
            // 有重复用户名则返回空
            throw new BusinessException(400, "抱歉，这个用户名被别人使用了，换一个试试吧~");
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
            throw new BusinessException(400, "用户名并发冲突！");
        }
        return user.getId();
    }

    @Override
    public LoginInfoResponse login(LoginRequest loginRequest, String ip) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        // 锁定期检查：已达失败上限则直接抛出429
        loginRateLimiter.checkLocked(username);
        loginRateLimiter.checkIpLocked(ip);
        // 1.根据用户名查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, loginRequest.getUsername()));
        if (user == null) {
            // 用户不存在：dummy BCrypt 拉平耗时 + 只记IP
            passwordEncoder.matches(password, DUMMY_HASH);
            loginRateLimiter.recordIpFailure(ip);
            log.warn("用户名不存在！");
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            // 密码错误，账号+IP 计数器+1 ，不做dummy
            loginRateLimiter.recordFailure(username);
            loginRateLimiter.recordIpFailure(ip);
            throw new BusinessException(400, "用户名或密码错误");
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
