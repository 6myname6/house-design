package com.housedesign.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：负责令牌的生成、解析与校验。
 * 密钥与过期时间从 application.yml 的 app.jwt 下读取。
 */
@Slf4j
@Component
public class JwtUtil {

    /** HMAC-SHA 签名密钥（由 secret 字符串派生，secret 需 >= 32 字节以满足 HS256） */
    private final SecretKey key;

    /** 令牌有效期（毫秒），yml 中配置为 7 天 */
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * 生成 JWT：subject 放 userId，自定义 claim 放 username。
     * 注意：payload 只放非敏感信息，绝不放密码。
     */
    public String createToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验令牌（验签 + 验过期）。
     * 令牌非法/过期/被篡改时会抛 JwtException 系列异常，由调用方决定如何处理。
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** 从令牌中取出用户 ID */
    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    /** 从令牌中取出用户名 */
    public String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    /**
     * 校验令牌是否合法（供拦截器使用）。
     * 任何解析异常都视为非法，返回 false，不向上抛出。
     */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            // 只记录异常类型，不打印 token 本身，避免令牌泄露进日志
            log.warn("JWT 校验失败：{}", e.getClass().getSimpleName());
            return false;
        }
    }
}
