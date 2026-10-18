package com.housedesign.Util;

import java.util.Collections;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisAtomicCounter {
    private final StringRedisTemplate stringRedisTemplate;
    private static final DefaultRedisScript<Long> script = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1])\n" + //
                    "if count == 1 then\n" + //
                    "    redis.call('EXPIRE', KEYS[1], ARGV[1])\n" + //
                    "end\n" + //
                    "return count",
            Long.class);

    public long incrWithTtl(String key, long ttlSeconds) {
        return stringRedisTemplate.execute(script, Collections.singletonList(key), String.valueOf(ttlSeconds));
    }
}
