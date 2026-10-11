package com.housedesign.common;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoginRateLimiter {
    private static final String KEY_PREFIX = "login:fail:";// 登录计数key前缀
    private static final String IPKEY_PREFIX = "login:ip:fail:";
    private final StringRedisTemplate stringRedisTemplate;
    private static final String FAIL_KEY_PREFIX = "sms:fail:login:";
    @Value("${app.sms.max-verify-fail-count:5}")
    private int maxVerifyFailCount;
    @Value("${app.login-limit.max-fail-count:5}")
    private int maxFailCount;
    @Value("${app.login-limit.lock-seconds:600}")
    private long lockSeconds;

    @Value("${app.login-limit.ip-max-fail-count:20}")
    private int ipMaxFailCount;
    @Value("${app.login-limit.ip-lock-seconds:600}")
    private long ipLockSeconds;

    // redis脚本
    private static final DefaultRedisScript script = new DefaultRedisScript<Long>(
            "local count = redis.call('INCR', KEYS[1])\n" + //
                    "if count == 1 then\n" + //
                    "    redis.call('EXPIRE', KEYS[1], ARGV[1])\n" + //
                    "end\n" + //
                    "return count",
            Long.class);

    // 类内方法
    private String buildLockMessage(String key) {
        Long ttl = stringRedisTemplate.getExpire(key);
        long remainSeconds = (ttl != null && ttl > 0 ? ttl : lockSeconds);
        long minutes = (remainSeconds + 59) / 60;

        return "登录失败次数达上限,请" + minutes + "分钟后重试";
    }

    // 基于账号密码
    // 判断账号是否在锁定期，已锁定则直接抛出429异常
    public void checkLocked(String username) {
        // 1.拼出完整key：前缀+username
        String key = KEY_PREFIX + username;
        // 2.查计数对应redis-cli 的GET
        String value = stringRedisTemplate.opsForValue().get(key);
        // 3.无记录则放行
        if (value == null) {
            return;
        }
        // 4.value强转
        try {
            int count = Integer.parseInt(value);
            // 5.判断是否超计数
            if (count >= maxFailCount) {
                log.warn("username:{}登录失败{}次,仍在锁定期", username, count);
                throw new BusinessException(429, buildLockMessage(key));
            }
        } catch (NumberFormatException e) {
            log.warn("发现脏记录");
            stringRedisTemplate.delete(key);
        }

    }

    // 记录失败次数
    public void recordFailure(String username) {
        // 1.拼出完整key：前缀+username
        String key = KEY_PREFIX + username;
        // 2.计数+1
        // 3.仅首次失败设置过期时间
        Long failCount = (Long) stringRedisTemplate.execute(script, Collections.singletonList(key),
                String.valueOf(lockSeconds));

        // 4.输出日志
        log.info("username:{}登录失败{}次", username, failCount);
        if (failCount != null && failCount >= maxFailCount) {
            log.warn("username:{}登录失败{}次,已锁定", username, failCount);
            throw new BusinessException(429, buildLockMessage(key));
        }
    }

    // 登录成功，清除失败次数
    public void clearOnSuccess(String username) {
        // 1.拼出完整key：前缀+username
        String key = KEY_PREFIX + username;
        // 2.清除记录
        stringRedisTemplate.delete(key);
    }

    // 基于验证码
    // 判断账号是否在锁定期，已锁定则直接抛出429异常
    public void smsCheckLocked(String phone) {
        // 1.拼出完整key：前缀+phone
        String key = FAIL_KEY_PREFIX + phone;
        // 2.查计数对应redis-cli 的GET
        String value = stringRedisTemplate.opsForValue().get(key);
        // 3.无记录则放行
        if (value == null) {
            return;
        }
        // 4.value强转
        int count = Integer.parseInt(value);
        // 5.判断是否超计数
        if (count >= maxVerifyFailCount) {
            log.warn("验证码登录失败{}次,仍在锁定期", count);
            throw new BusinessException(429, "验证码错误次数过多，请重新获取验证码");
        }
    }

    // 记录失败次数
    public void smsRecordFailure(String phone) {
        // 1.拼出完整key：前缀+phone
        String key = FAIL_KEY_PREFIX + phone;
        // 2.计数+1
        // 3.仅首次失败设置过期时间
        Long failCount = (Long) stringRedisTemplate.execute(script, Collections.singletonList(key),
                "300");
        // 4.输出日志
        log.info("验证码登录失败{}次", failCount);
        if (failCount != null && failCount >= maxVerifyFailCount) {
            log.warn("登录失败{}次,已锁定", failCount);
            throw new BusinessException(429, "验证码错误次数过多，请重新获取验证码");
        }
    }

    // 登录成功，清除失败次数
    public void smsClearOnSuccess(String phone) {
        // 1.拼出完整key：前缀+username
        String key = FAIL_KEY_PREFIX + phone;
        // 2.清除记录
        stringRedisTemplate.delete(key);
    }

    public void checkIpLocked(String ip) {
        // 1.拼出完整key：前缀+username
        String key = IPKEY_PREFIX + ip;
        // 2.查计数对应redis-cli 的GET
        String value = stringRedisTemplate.opsForValue().get(key);
        // 3.无记录则放行
        if (value == null) {
            return;
        }
        // 4.value强转
        try {
            int count = Integer.parseInt(value);
            // 5.判断是否超计数
            if (count >= ipMaxFailCount) {
                log.warn("同一IP登录失败{}次,仍在锁定期", count);
                throw new BusinessException(429, buildLockMessage(key));
            }
        } catch (NumberFormatException e) {
            log.warn("发现脏记录");
            stringRedisTemplate.delete(key);
        }
    }

    public void recordIpFailure(String ip) {
        // 1.拼出完整key：前缀+phone
        String key = IPKEY_PREFIX + ip;
        // 2.计数+1
        // 3.仅首次失败设置过期时间
        Long failCount = (Long) stringRedisTemplate.execute(script, Collections.singletonList(key),
                String.valueOf(ipLockSeconds));
        // 4.输出日志
        log.info("当前IP登录失败{}次", failCount);
        if (failCount != null && failCount >= ipMaxFailCount) {
            log.warn("当前IP登录失败{}次,已锁定", failCount);
            throw new BusinessException(429, "当前IP登录失败" + failCount + "次,已锁定");
        }
    }

}
