package com.housedesign.common;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.housedesign.Service.EmailService;
import com.housedesign.Utils.RedisAtomicCounter;
import com.housedesign.entity.User;
import com.housedesign.mapper.UserMapper;

import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class EmailCodeService {
    private static final String CODE_KEY_PREFIX = "email:code:login:";
    private static final String LIMIT_KEY_PREFIX = "email:limit:login:";
    private static final String DAILY_EMAIL_PREFIX = "email:daily:email:";
    private static final String DAILY_IP_PREFIX = "email:daily:ip:";
    private static final String FAIL_KEY_PREFIX = "email:fail:login:";

    @Value("${app.email.code-ttl-seconds}")
    private long emailCodeTtlseconds;

    @Value("${app.email.daily-email-limit}")
    private int dailyEmailLimit;

    @Value("${app.email.daily-ip-limit}")
    private int emailIpLimit;

    @Value("${app.email.daily-window-seconds}")
    private long emailWindowSeconds;

    @Value("${app.email.resend-interval-seconds}")
    private long emailResendIntervalSeconds;

    private final StringRedisTemplate stringRedisTemplate;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final LoginRateLimiter loginRateLimiter;
    private final RedisAtomicCounter redisAtomicCounter;

    // 邮箱发码与限制
    public void sendCode(String email, String ip, String scene) {
        // 拼key

        String limitLoginKey = LIMIT_KEY_PREFIX + email;// 验证码连续发送限制
        String dailyLimitKey = DAILY_EMAIL_PREFIX + email;// 邮箱日限
        String dailyIpLimitKey = DAILY_IP_PREFIX + ip;// IP日限
        String failCodeKey = FAIL_KEY_PREFIX + email;// 邮箱登录失败计数key

        Long userExists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        // scene:场景
        if ("register".equals(scene)) {
            // 注册场景
            if (userExists > 0) {
                log.warn("发码被拒：邮箱{}已注册，场景=register", email);
                throw new BusinessException(400, "该邮箱已注册，请直接登录");
            }
        }
        if ("login".equals(scene)) {
            // 登录场景
            if (userExists == 0) {
                log.warn("发码被拒：邮箱{}未注册，场景=login", email);
                throw new BusinessException(400, "该邮箱未注册，请先注册");
            }
        }
        Boolean isSended = stringRedisTemplate.opsForValue().setIfAbsent(limitLoginKey, "1", emailResendIntervalSeconds,
                TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(isSended)) {
            log.warn("发送过于频繁");
            Long ttl = stringRedisTemplate.getExpire(limitLoginKey);
            throw new BusinessException(429, "发送过于频繁，请" + ttl + "秒后重试");
        }
        // 查邮箱发码日限：第二参数是计数 key 的 TTL（24h 窗口），不是阈值
        long emailSendTimes = redisAtomicCounter.incrWithTtl(dailyLimitKey, emailWindowSeconds);
        long ipSendTimes = redisAtomicCounter.incrWithTtl(dailyIpLimitKey, emailWindowSeconds);
        if (emailSendTimes > dailyEmailLimit) {
            log.warn("邮箱{}当日发码超限", email);
            throw new BusinessException(429, "今日验证码发送次数已达上限，请明天再试");
        }
        if (ipSendTimes > emailIpLimit) {
            log.warn("当前IP发码超限");
            throw new BusinessException(429, "当前网络今日请求次数过多，请明天再试");
        }
        // 生成邮箱验证码
        String code = RandomUtil.randomNumbers(6);
        String emailCodeKey = CODE_KEY_PREFIX + email;
        stringRedisTemplate.opsForValue().set(emailCodeKey, code, emailCodeTtlseconds, TimeUnit.SECONDS);
        // 清除失败次数
        loginRateLimiter.clearCodeFailure(failCodeKey);
        // 调emailService发送验证码
        try {
            emailService.send(email, code);
        } catch (Exception e) {
            stringRedisTemplate.delete(limitLoginKey);
            throw e;
        }
    }

    public boolean verify(String email, String inputCode) {
        String failCodeKey = FAIL_KEY_PREFIX + email;// 邮箱登录失败计数key
        String emailCodeKey = CODE_KEY_PREFIX + email;// 验证码key
        loginRateLimiter.checkCodeLocked(failCodeKey);
        String getCodeKey = stringRedisTemplate.opsForValue().get(emailCodeKey);
        if (getCodeKey == null || !inputCode.equals(getCodeKey)) {
            loginRateLimiter.recordCodeFailure(failCodeKey);
            return false;
        }
        stringRedisTemplate.delete(emailCodeKey);
        loginRateLimiter.clearCodeFailure(failCodeKey);
        return true;
    }
}
