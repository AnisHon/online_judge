package com.anishan.user.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SseRedisUtil {

    private final StringRedisTemplate redisTemplate;

    private String getSseKey(Long userId) {
        return "sse:userid:" + userId;
    }

    public void saveUserSession(Long userId, String sessionId) {
        String sseKey = getSseKey(userId);
        SetOperations<String, String> setOp = redisTemplate.opsForSet();
        setOp.add(sseKey, sessionId);
        redisTemplate.expire(sseKey, 31, TimeUnit.SECONDS);
    }

    public Set<String> getUserSessions(Long userId) {
        return redisTemplate.opsForSet().members(getSseKey(userId));
    }

    public void removeUserSession(Long userId, String sessionId) {
        String sseKey = getSseKey(userId);
        redisTemplate.opsForSet().remove(sseKey, sessionId);
    }

}
