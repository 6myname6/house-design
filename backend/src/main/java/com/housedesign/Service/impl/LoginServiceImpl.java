package com.housedesign.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.housedesign.Service.LoginService;
import com.housedesign.Utils.JwtUtil;
import com.housedesign.common.ActiveTokenService;
import com.housedesign.common.BusinessException;
import com.housedesign.common.EmailCodeService;
import com.housedesign.common.LoginRateLimiter;
import com.housedesign.dto.request.EmailLoginRequest;
import com.housedesign.dto.request.LoginRequest;
import com.housedesign.dto.request.RegisterRequest;
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
    private final EmailCodeService emailCodeService;
    private final ActiveTokenService activeTokenService;
    /** 用户不存在时用于拉平响应耗时的假 BCrypt 哈希（cost=10），仅用于时序对齐 */
    private static final String DUMMY_HASH = "$2a$10$jOQld7IG2qbJtpYYxx1LF.wO0tAhn08mUYcPvkR7tj9nhS07Pm3ce";

    private LoginInfoResponse issueToken(User user) {
        String token;
        token = jwtUtil.createToken(user.getId(), user.getUsername());
        activeTokenService.save(user.getId(), token);
        return new LoginInfoResponse(user.getId(), token);
    }

    @Override
    public Long register(RegisterRequest registerRequest) {
        String email = registerRequest.getEmail();
        String username = registerRequest.getUsername();

        // 1.校验邮件验证码（通过即一次性删除）
        if (!emailCodeService.verify(email, registerRequest.getCode())) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        // 2.用户名占用预检
        Long nameCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (nameCount > 0) {
            throw new BusinessException(400, "抱歉，这个用户名被别人使用了，换一个试试吧~");
        }
        // 3.邮箱占用预检
        Long identifierCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (identifierCount > 0) {
            throw new BusinessException(400, "该邮箱已注册，请直接登录");
        }
        // 4.建档：用户名 + 邮箱 + BCrypt 加密密码
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setNickname("筑梦家er_" + RandomUtil.randomString(6));
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发下唯一索引兜底：按邮箱/用户名重查，给出准确的冲突提示
            log.warn("注册并发冲突：username={}, email={}", username, email);
            User existing = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, email));
            if (existing != null) {
                throw new BusinessException(400, "该邮箱已注册，请直接登录");
            }
            throw new BusinessException(400, "抱歉，这个用户名被别人使用了，换一个试试吧~");
        }
        return user.getId();
    }

    @Override
    public LoginInfoResponse loginByPassword(LoginRequest loginRequest, String ip) {
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
        return issueToken(user);
    }

    @Override
    public LoginInfoResponse loginByEmail(EmailLoginRequest emailLoginRequest) {
        String email = emailLoginRequest.getEmail();
        String code = emailLoginRequest.getCode();
        // 1.校验验证码（错误 5 次锁定由 EmailCodeService 内部处理）
        if (!emailCodeService.verify(email, code)) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        // 2.按邮箱查用户：登录接口不负责建档
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new BusinessException(400, "该邮箱未注册，请先注册");
        }
        return issueToken(user);
    }

}
