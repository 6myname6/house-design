package com.housedesign.common;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.housedesign.Service.EmailService;
import com.housedesign.entity.User;
import com.housedesign.mapper.UserMapper;

import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 邮箱绑定/换绑（§10.8）：登录态下凭新邮箱验证码更新当前用户 email。
 * 安全要点：验证码 key 按 userId 归属（防越权替他人绑定）；新邮箱发码前与提交时两次占用校验，
 * 并发由 uk_email 唯一索引兜底；换绑只需新邮箱验证码（2026-10-17 决策，登录态即身份）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailBindService {
    private static final String CODE_KEY_PREFIX = "bind:email:code:";
    private static final String LIMIT_KEY_PREFIX = "bind:email:limit:";
    private static final String DAILY_KEY_PREFIX = "bind:email:daily:";
    private static final String FAIL_KEY_PREFIX = "bind:email:fail:";

    @Value("${app.email-bind.code-ttl-seconds:300}")
    private long codeTtlSeconds;

    @Value("${app.email-bind.resend-interval-seconds:60}")
    private long resendIntervalSeconds;

    @Value("${app.email-bind.daily-limit:10}")
    private int dailyLimit;

    @Value("${app.email-bind.daily-window-seconds:86400}")
    private long dailyWindowSeconds;

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisAtomicCounter redisAtomicCounter;
    private final LoginRateLimiter loginRateLimiter;
    private final UserMapper userMapper;
    private final EmailService emailService;

    /** 发送绑定邮箱验证码；email 必须未被任何账号占用 */
    public void sendBindCode(String email) {
        Long userId = UserContext.getUserId();

        // 占用预检：被任意账号（含自己当前邮箱——换绑发同邮箱无意义）绑定则拒绝，不发信不占位
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (count > 0) {
            log.warn("绑定发码被拒：邮箱{}已被占用，userId={}", email, userId);
            throw new BusinessException(400, "该邮箱已被其他账号绑定");
        }

        String limitKey = LIMIT_KEY_PREFIX + userId;
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(limitKey, "1", resendIntervalSeconds,
                TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(acquired)) {
            Long remain = stringRedisTemplate.getExpire(limitKey);
            throw new BusinessException(429, "发送过于频繁，请" + remain + "秒后重试");
        }
        long dailyCount = redisAtomicCounter.incrWithTtl(DAILY_KEY_PREFIX + userId, dailyWindowSeconds);
        if (dailyCount > dailyLimit) {
            throw new BusinessException(429, "今日验证码发送次数已达上限，请明天再试");
        }

        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(CODE_KEY_PREFIX + userId, code, codeTtlSeconds, TimeUnit.SECONDS);
        loginRateLimiter.clearCodeFailure(FAIL_KEY_PREFIX + userId);

        try {
            emailService.send(email, code);
            log.info("绑定邮箱验证码已发送：userId={}", userId);
        } catch (Exception e) {
            stringRedisTemplate.delete(limitKey);
            throw e;
        }
    }

    /** 校验验证码并完成绑定/换绑，返回提示语（绑定成功/更换成功） */
    public String bindEmail(String email, String inputCode) {
        Long userId = UserContext.getUserId();
        String failKey = FAIL_KEY_PREFIX + userId;
        loginRateLimiter.checkCodeLocked(failKey);

        String codeKey = CODE_KEY_PREFIX + userId;
        String savedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (savedCode == null || !savedCode.equals(inputCode)) {
            loginRateLimiter.recordCodeFailure(failKey);
            throw new BusinessException(400, "验证码错误或已过期");
        }

        // 提交时二次占用校验（发码后可能已被并发绑定）
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (count > 0) {
            throw new BusinessException(400, "该邮箱已被其他账号绑定");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        boolean wasBound = user.getEmail() != null && !user.getEmail().isBlank();
        user.setEmail(email);
        try {
            userMapper.updateById(user);
        } catch (DuplicateKeyException e) {
            // uk_email 并发兜底
            log.warn("邮箱绑定并发冲突：userId={}, email={}", userId, email);
            throw new BusinessException(400, "该邮箱已被其他账号绑定");
        }

        // 验证码一次性 + 清错误计数
        stringRedisTemplate.delete(codeKey);
        loginRateLimiter.clearCodeFailure(failKey);
        log.info("用户{}邮箱{}成功：{}", userId, wasBound ? "换绑" : "绑定", email);
        return wasBound ? "邮箱更换成功" : "邮箱绑定成功";
    }
}
