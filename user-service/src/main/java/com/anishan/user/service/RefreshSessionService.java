package com.anishan.user.service;

import com.anishan.commons.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Server-side refresh session registry. The refresh JWT is never stored here;
 * Redis stores only identifiers needed for validation and revocation.
 */
@Service
@RequiredArgsConstructor
public class RefreshSessionService {

    private static final String SESSION_PREFIX = "user-service:refresh-session:";
    private static final String USER_INDEX_PREFIX = "user-service:refresh-sessions:user:";
    private static final long SESSION_TTL_SECONDS = JwtUtil.REFRESH_EXPIRE_DAYS * 24L * 60L * 60L;

    private final StringRedisTemplate redisTemplate;

    public void create(String sessionId, Long userId, String tokenId) {
        Map<String, String> session = new HashMap<>();
        session.put("userId", String.valueOf(userId));
        session.put("tokenId", tokenId);
        session.put("expiresAt", String.valueOf(System.currentTimeMillis() + SESSION_TTL_SECONDS * 1000L));

        String sessionKey = sessionKey(sessionId);
        redisTemplate.opsForHash().putAll(sessionKey, session);
        redisTemplate.expire(sessionKey, SESSION_TTL_SECONDS, TimeUnit.SECONDS);

        String userIndexKey = userIndexKey(userId);
        redisTemplate.opsForSet().add(userIndexKey, sessionId);
        redisTemplate.expire(userIndexKey, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
    }

    public boolean isActive(JwtUtil.RefreshClaims claims) {
        Map<Object, Object> session = redisTemplate.opsForHash().entries(sessionKey(claims.getSessionId()));
        if (session.isEmpty()) return false;
        return String.valueOf(claims.getUserId()).equals(String.valueOf(session.get("userId")))
                && claims.getTokenId().equals(String.valueOf(session.get("tokenId")));
    }

    public void revoke(JwtUtil.RefreshClaims claims) {
        revoke(claims.getSessionId(), claims.getUserId());
    }

    public void revoke(String sessionId, Long userId) {
        redisTemplate.delete(sessionKey(sessionId));
        redisTemplate.opsForSet().remove(userIndexKey(userId), sessionId);
    }

    /** Hook for password changes, account bans and future device management. */
    public void revokeAll(Long userId) {
        String indexKey = userIndexKey(userId);
        Set<String> sessions = redisTemplate.opsForSet().members(indexKey);
        for (String sessionId : sessions == null ? Collections.<String>emptySet() : sessions) {
            redisTemplate.delete(sessionKey(sessionId));
        }
        redisTemplate.delete(indexKey);
    }

    private String sessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }

    private String userIndexKey(Long userId) {
        return USER_INDEX_PREFIX + userId;
    }
}
