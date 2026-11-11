package com.acme.notify.dispatcher;

import com.acme.notify.common.Category;
import com.acme.notify.common.Channel;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/** Atomic sliding-window limiter: ZSET of timestamps per user, channel and category. */
@Component
@RequiredArgsConstructor
public class SlidingWindowRateLimiter {

    private static final RedisScript<Long> SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
            if redis.call('ZCARD', key) < limit then
              redis.call('ZADD', key, now, ARGV[4])
              redis.call('PEXPIRE', key, window)
              return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;

    public boolean tryAcquire(String userId, Channel channel, Category category, int limit, Duration window) {
        String key = "notif:rl:{%s}:%s:%s".formatted(userId, channel, category);
        Long ok = redis.execute(SCRIPT, List.of(key),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(window.toMillis()),
                String.valueOf(limit),
                UUID.randomUUID().toString());
        return Long.valueOf(1L).equals(ok);
    }
}
