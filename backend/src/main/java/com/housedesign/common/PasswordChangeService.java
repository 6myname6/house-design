package com.housedesign.common;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.housedesign.Service.EmailService;
import com.housedesign.Service.SmsService;
import com.housedesign.Util.RedisAtomicCounter;
import com.housedesign.Util.UserContext;
import com.housedesign.entity.User;
import com.housedesign.mapper.UserMapper;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 修改密码验证码组件（A-5）：已登录用户凭本人绑定手机/邮箱收到的验证码改密。
 * 安全要点：收件地址一律按 UserContext 中的 userId 查库得到，绝不使用请求体传入的手机/邮箱；
 * Redis key 独立 pwd:* 前缀，与登录/注册验证码场景隔离，防越场重放。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordChangeService {
    private static final String CODE_KEY_PREFIX = "pwd:code:";
    private static final String LIMIT_KEY_PREFIX = "pwd:limit:";
    private static final String DAILY_KEY_PREFIX = "pwd:daily:";
    private static final String FAIL_KEY_PREFIX = "pwd:fail:";

    @Value("${app.password-change.code-ttl-seconds:300}")
    private long codeTtlSeconds;

    @Value("${app.password-change.resend-interval-seconds:60}")
    private long resendIntervalSeconds;

    @Value("${app.password-change.daily-limit:10}")
    private int dailyLimit;

    @Value("${app.password-change.daily-window-seconds:86400}")
    private long dailyWindowSeconds;

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisAtomicCounter redisAtomicCounter;
    private final LoginRateLimiter loginRateLimiter;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SmsService smsService;
    private final EmailService emailService;
    private final TokenBlackListService tokenBlackListService;

    /** 发送修改密码验证码；channel=sms/email，收件地址由后端按当前登录用户查库决定 */
    public void sendChangeCode(String channel) {
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        // 取本人该通道的绑定地址：未绑定直接拒绝，不发信、不占位
        String target = "sms".equals(channel) ? user.getPhone() : user.getEmail();
        String channelName = "sms".equals(channel) ? "手机" : "邮箱";
        if (StrUtil.isBlank(target)) {
            throw new BusinessException(400, "当前账号未绑定" + channelName + "，无法使用该方式验证");
        }

        String limitKey = LIMIT_KEY_PREFIX + userId;
        String dailyKey = DAILY_KEY_PREFIX + userId;
        // 60s 发送间隔：两通道共用一把锁，防切通道刷
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(limitKey, "1", resendIntervalSeconds,
                TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(acquired)) {
            Long remain = stringRedisTemplate.getExpire(limitKey);
            throw new BusinessException(429, "发送过于频繁，请" + remain + "秒后重试");
        }
        // 当日总量（两通道合计）
        long dailyCount = redisAtomicCounter.incrWithTtl(dailyKey, dailyWindowSeconds);
        if (dailyCount > dailyLimit) {
            throw new BusinessException(429, "今日验证码发送次数已达上限，请明天再试");
        }

        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(CODE_KEY_PREFIX + channel + ":" + userId, code, codeTtlSeconds,
                TimeUnit.SECONDS);
        // 重新发码 = 重新给 5 次错误机会
        loginRateLimiter.clearCodeFailure(FAIL_KEY_PREFIX + userId);

        try {
            if ("sms".equals(channel)) {
                smsService.send(target, code);
            } else {
                emailService.send(target, code);
            }
            log.info("改密验证码已发送：userId={}, channel={}", userId, channel);
        } catch (Exception e) {
            // 通道发送失败：删 60s 占位允许立即重试（日计数不回滚，防刷）
            stringRedisTemplate.delete(limitKey);
            throw e;
        }
    }

    /**
     * 校验验证码并修改密码；成功后把当前 token 拉黑，强制重新登录。
     * 
     * @param rawToken 原始 JWT（不含 Bearer 前缀），由 Controller 从请求头截取传入
     */
    public void changePassword(String channel, String inputCode, String newPassword, String rawToken) {
        Long userId = UserContext.getUserId();
        String failKey = FAIL_KEY_PREFIX + userId;
        // 锁定检查：错误已达 5 次直接 429
        loginRateLimiter.checkCodeLocked(failKey);

        String codeKey = CODE_KEY_PREFIX + channel + ":" + userId;
        String savedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (savedCode == null || !savedCode.equals(inputCode)) {
            // 错误计数 +1（内部达 5 次直接抛 429），保留验证码允许有效期内重试
            loginRateLimiter.recordCodeFailure(failKey);
            throw new BusinessException(400, "验证码错误或已过期");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        // 验证码一次性 + 清错误计数
        stringRedisTemplate.delete(codeKey);
        loginRateLimiter.clearCodeFailure(failKey);
        // 当前 token 立即失效（其余端旧 token 同样在过期前失效）
        tokenBlackListService.blacklist(rawToken);
        log.info("用户{}修改密码成功，旧 token 已拉黑", userId);
    }
}
