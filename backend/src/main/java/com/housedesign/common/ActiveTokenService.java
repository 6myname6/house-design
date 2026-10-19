package com.housedesign.common;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.housedesign.Utils.JwtUtil;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ActiveTokenService {
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;
    private static final String KEY_PREFIX = "login:active:";

    // 算指纹
    public void save(Long userId, String token) {
        String key = KEY_PREFIX + userId;
        String value = TokenBlackListService.sha256Hex(token);
        // 拿ttl
        // 1.取token
        Claims claims = jwtUtil.parseToken(token);
        Date expiration = claims.getExpiration();
        // 2.算ttl
        long ttlMs = expiration.getTime() - System.currentTimeMillis();
        if (ttlMs <= 0) {
            ttlMs = 1000L;
        }
        stringRedisTemplate.opsForValue().set(key, value, ttlMs, TimeUnit.MILLISECONDS);
    }

    public boolean matchesActive(Long userId, String token) {
        try {
            String key = KEY_PREFIX + userId;
            String getSha256 = stringRedisTemplate.opsForValue().get(key);
            if (getSha256 == null) {
                return true;
            }
            String nowSha256 = TokenBlackListService.sha256Hex(token);
            if (nowSha256.equals(getSha256)) {
                return true;
            }
        } catch (Exception e) {
            log.error("查询互踢账本失效，执行fail-open放行", e);
            return true;
        }
        return false;
    }

    // 登出时删除账本
    public void delete(Long userId) {
        stringRedisTemplate.delete(KEY_PREFIX + userId);
    }
}
