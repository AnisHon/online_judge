package com.anishan.user.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SseRedisUtil {

    private final StringRedisTemplate redisTemplate;

    private String getSseKey(Long userId) {
        return "sse:userid:" + userId;
    }

    public void saveUserSession(Long userId, String sessionId) {
        String sseKey = getSseKey(userId);
        redisTemplate.opsForSet().add(sseKey, sessionId);
    }

    public Set<String> getUserSessions(Long userId) {
        return redisTemplate.opsForSet().members(getSseKey(userId));
    }

    public void removeUserSession(Long userId, String sessionId) {
        String sseKey = getSseKey(userId);
        redisTemplate.opsForSet().remove(sseKey, sessionId);
    }
}
