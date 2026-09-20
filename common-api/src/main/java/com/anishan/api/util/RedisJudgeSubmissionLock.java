package com.anishan.api.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 限制同一用户同时进入 OJ 判题队列。
 *
 * <p>锁有硬 TTL，即使判题机宕机或内部回调丢失，也不会让用户永久无法提交。
 * 释放时校验令牌，防止旧判题回调删除 TTL 过期后新提交获得的锁。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisJudgeSubmissionLock {

    /** 必须小于等于 15 秒，作为异常情况下的最终兜底。 */
    public static final long LOCK_TIMEOUT_SECONDS = 12L;
    private static final String KEY_PREFIX = "judge:submission:lock:";

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public String tryAcquire(Long userId) {
        if (userId == null) {
            return null;
        }
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                getKey(userId), token, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS
        );
        return Boolean.TRUE.equals(acquired) ? token : null;
    }

    public void release(Long userId, String token) {
        if (userId == null || token == null) {
            return;
        }
        try {
            redisTemplate.execute(
                    RELEASE_SCRIPT,
                    Collections.singletonList(getKey(userId)),
                    token
            );
        } catch (RuntimeException e) {
            // 释放失败时由 Redis TTL 兜底，不能让判题回调因清理锁失败而重复重试。
            log.warn("释放用户判题锁失败，等待 TTL 自动过期，userId={}", userId, e);
        }
    }

    private String getKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
