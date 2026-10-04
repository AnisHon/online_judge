package com.anishan.content.service;

import com.anishan.commons.exception.ApiStatusException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

/** Atomic per-user first-attempt limiter. Redis failure is fail-closed. */
@Component
public class CommentRateLimiter {
    static final int MAX_ATTEMPTS = 10;
    static final int WINDOW_SECONDS = 60;
    static final DefaultRedisScript<Long> LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); " +
            "if count == 1 or redis.call('TTL', KEYS[1]) < 0 then " +
            "redis.call('EXPIRE', KEYS[1], ARGV[2]); end; " +
            "if count > tonumber(ARGV[1]) then return 0; end; return count;", Long.class);

    private final StringRedisTemplate redisTemplate;

    public CommentRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void acquire(Long userId) {
        if (userId == null || userId <= 0) throw new ApiStatusException(401, "请先登录");
        final Long count;
        try {
            count = redisTemplate.execute(LIMIT_SCRIPT,
                    Collections.singletonList("content:comment:write:" + userId),
                    String.valueOf(MAX_ATTEMPTS), String.valueOf(WINDOW_SECONDS));
        } catch (RuntimeException exception) {
            throw new ApiStatusException(503, "评论服务暂不可用，请稍后重试");
        }
        if (count == null) throw new ApiStatusException(503, "评论服务暂不可用，请稍后重试");
        if (count == 0L) throw new ApiStatusException(429, "评论操作过于频繁，请稍后重试");
    }
}
