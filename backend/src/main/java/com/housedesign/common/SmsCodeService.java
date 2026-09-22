package com.housedesign.common;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.housedesign.Service.SmsService;

import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class SmsCodeService {
    private static final String CODE_KEY_PREFIX = "sms:code:login:";
    private static final String LIMIT_KEY_PREFIX = "sms:limit:reset:";
    @Value("${app.sms.code-ttl-seconds:300}")
    private long codeTtlSeconds;
    @Value("${app.sms.resend-interval-seconds:60}")
    private long resendIntervalSeconds;
    private final StringRedisTemplate stringRedisTemplate;
    private final SmsService smsService;

    public void sendCode(String phone) {
        // 1.拼频控key
        String limitKey = LIMIT_KEY_PREFIX + phone;
        String codeKey = CODE_KEY_PREFIX + phone;
        if (stringRedisTemplate.hasKey(limitKey)) {
            log.warn("发送过于频繁");
            throw new BusinessException(429, "发送过于频繁，请" + stringRedisTemplate.getExpire(limitKey) + "秒后重试");
        }
        // 生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 写验证码
        stringRedisTemplate.opsForValue().set(codeKey, code, codeTtlSeconds, TimeUnit.SECONDS);
        // 写频控标记
        stringRedisTemplate.opsForValue().set(limitKey, "1", resendIntervalSeconds, TimeUnit.SECONDS);
        // 调用smsService
        smsService.send(phone, code);
    }

    // 校验验证码,正确则删除，并返回true，错误则/过期返回false（保留key）
    public boolean verify(String phone, String inputCode) {
        // 拼验证码
        String codeKey = CODE_KEY_PREFIX + phone;
        // 从redis取验证码
        String savedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (savedCode == null || !savedCode.equals(inputCode)) {
            // savedCode==null 为过期；不相等为错误
            return false;
        }
        stringRedisTemplate.delete(codeKey);
        return true;
    }
}
