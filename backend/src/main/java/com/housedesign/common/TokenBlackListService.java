package com.housedesign.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
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
public class TokenBlackListService {
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;
    private static final String KEY_PREFIX = "jwt:blacklist:";

    // 算指纹
    public static String sha256Hex(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
            String sha256Hex = HexFormat.of().formatHex(hashBytes);
            return sha256Hex;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前运行环境不支持SHA-256", e);
        }
    }

    private String buildKey(String token) {
        return KEY_PREFIX + sha256Hex(token);
    }

    /**
     * 将JWT添加到黑名单
     * 黑名单中存储JWT的指纹，过期时间为JWT的过期时间
     * 过期后，黑名单中的指纹将被删除
     * 
     * @param token JWT令牌
     */
    public void blacklist(String token) {
        Claims claims = jwtUtil.parseToken(token);
        Date expiration = claims.getExpiration();
        long ttlMs = expiration.getTime() - System.currentTimeMillis();
        if (ttlMs <= 0) {
            ttlMs = 1000L;
        }
        String key = buildKey(token);
        stringRedisTemplate.opsForValue().set(key, "1", ttlMs, TimeUnit.MILLISECONDS);
        log.info("剩余秒数：{}", ttlMs / 1000);
    }

    /**
     * 检查JWT是否在黑名单中
     * 若查询失败，执行fail-open策略，返回false
     * 
     * @param token JWT令牌
     * @return 如果JWT在黑名单中，返回true；否则返回false
     */
    public boolean isBlacklisted(String token) {
        try {
            Boolean exists = stringRedisTemplate.hasKey(buildKey(token));
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("查询JWT黑名单失败，执行fail-open放行", e);
            return false;
        }
    }
}
