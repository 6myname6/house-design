package com.housedesign.common;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.housedesign.Service.SmsService;
import com.housedesign.entity.User;
import com.housedesign.mapper.UserMapper;

import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsCodeService {
    private static final String CODE_KEY_PREFIX = "sms:code:login:";
    private static final String LIMIT_KEY_PREFIX = "sms:limit:login:";

    @Value("${app.sms.code-ttl-seconds:300}")
    private long codeTtlSeconds;

    @Value("${app.sms.resend-interval-seconds:60}")
    private long resendIntervalSeconds;

    @Value("${app.sms.daily-phone-limit:10}")
    private int dailyPhoneLimit;// 同手机号限制

    @Value("${app.sms.daily-ip-limit:20}")
    private int dailyIpLimit;// 同IP限制

    @Value("${app.sms.daily-window-seconds:86400}")
    private long dailyWindowSeconds;

    private final StringRedisTemplate stringRedisTemplate;
    private final SmsService smsService;
    private final LoginRateLimiter loginRateLimiter;
    private final RedisAtomicCounter redisAtomicCounter;
    private final UserMapper userMapper;

    public void sendCode(String phone, String ip, String scene) {
        // 0.按场景预检手机号注册状态：放在所有频控写入之前，提示后用户可立即在正确页面重发
        Long phoneExists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if ("register".equals(scene) && phoneExists > 0) {
            log.warn("发码被拒：手机号{}已注册，场景=register", phone);
            throw new BusinessException(400, "该手机号已注册，请直接登录");
        }
        if ("login".equals(scene) && phoneExists == 0) {
            log.warn("发码被拒：手机号{}未注册，场景=login", phone);
            throw new BusinessException(400, "该手机号未注册，请先注册");
        }
        // 1.拼频控key
        String limitKey = LIMIT_KEY_PREFIX + phone;
        String codeKey = CODE_KEY_PREFIX + phone;
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(limitKey, "1", resendIntervalSeconds,
                TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(acquired)) {
            // 占位失败 = 60 秒内已发送过，取剩余TTL拼提示
            Long remain = stringRedisTemplate.getExpire(limitKey);
            throw new BusinessException(429, "发送过于频繁，请" + remain + "秒后重试");
        }

        long phoneCount = redisAtomicCounter.incrWithTtl("sms:daily:phone:" + phone, dailyWindowSeconds);
        if (phoneCount > dailyPhoneLimit) {
            throw new BusinessException(429, "今日验证码发送次数已达上限，请明天再试");
        }
        long ipCount = redisAtomicCounter.incrWithTtl("sms:daily:ip:" + ip, dailyWindowSeconds);
        if (ipCount > dailyIpLimit) {
            throw new BusinessException(429, "当前网络今日请求次数过多，请明天再试");
        }
        // 生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 写验证码
        stringRedisTemplate.opsForValue().set(codeKey, code, codeTtlSeconds, TimeUnit.SECONDS);
        // 清除次数
        loginRateLimiter.smsClearOnSuccess(phone);
        // 调用smsService
        try {
            smsService.send(phone, code);
        } catch (Exception e) {
            log.warn("发送失败");
            stringRedisTemplate.delete(limitKey);
            throw e;
        }
    }

    // 校验验证码,正确则删除，并返回true，错误/过期返回false（保留key）
    public boolean verify(String phone, String inputCode) {
        loginRateLimiter.smsCheckLocked(phone);
        // 拼key
        String codeKey = CODE_KEY_PREFIX + phone;
        // 从redis取验证码
        String savedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (savedCode == null || !savedCode.equals(inputCode)) {
            // savedCode==null 为过期；不相等为错误
            loginRateLimiter.smsRecordFailure(phone);
            return false;
        }
        stringRedisTemplate.delete(codeKey);
        loginRateLimiter.smsClearOnSuccess(phone);
        return true;
    }
}
