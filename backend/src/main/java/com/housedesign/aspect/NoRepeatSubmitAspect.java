package com.housedesign.aspect;

import java.lang.reflect.Method;
import java.time.Duration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.housedesign.annotation.NoRepeatSubmit;
import com.housedesign.common.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Aspect
@RequiredArgsConstructor
public class NoRepeatSubmitAspect {
    private static final String KEY_PREFIX = "repeat:";
    private final StringRedisTemplate stringRedisTemplate;

    @Pointcut("@annotation(com.housedesign.annotation.NoRepeatSubmit)")

    public void repeatSubmitPointcut() {
    }

    @Around("repeatSubmitPointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        org.aspectj.lang.reflect.MethodSignature signature = (org.aspectj.lang.reflect.MethodSignature) pjp
                .getSignature();
        // 拿method/interval
        Method method = signature.getMethod();
        NoRepeatSubmit annotation = method.getAnnotation(NoRepeatSubmit.class);
        long interval = annotation.interval();
        log.info("防重切面命中，interval={}秒", interval);
        // 取request
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs.getRequest();
        String clientIp = getClientIp(request);
        // 拼key
        String key = KEY_PREFIX + request.getRequestURI() + ":" + clientIp;
        // SETNX占位
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(interval));
        if (Boolean.FALSE.equals(acquired)) {
            log.warn("重复提交被拦截：{}", key);
            throw new BusinessException(409, "操作太频繁，请稍后再试");
        }
        // 执行业务
        try {
            return pjp.proceed();
        } catch (Throwable e) {
            stringRedisTemplate.delete(key);
            throw e;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            // 多级代理时取第一个
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
