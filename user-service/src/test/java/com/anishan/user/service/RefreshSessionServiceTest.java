package com.anishan.user.service;

import com.anishan.commons.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshSessionServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private RefreshSessionService service;

    @Test
    void createStoresOnlyRefreshSessionIdentifiers() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        service.create("session-1", 42L, "token-id-1");

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(eq("user-service:refresh-session:session-1"), captor.capture());
        Map<String, String> stored = captor.getValue();
        assertEquals("42", stored.get("userId"));
        assertEquals("token-id-1", stored.get("tokenId"));
        assertTrue(stored.containsKey("expiresAt"));
        assertFalse(stored.values().stream().anyMatch(value -> value.contains("eyJ")),
                "Redis must not contain the refresh JWT");
        verify(setOperations).add("user-service:refresh-sessions:user:42", "session-1");
    }

    @Test
    void activeSessionRequiresMatchingUserAndTokenId() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("user-service:refresh-session:session-1"))
                .thenReturn(Map.of("userId", "42", "tokenId", "token-id-1"));

        JwtUtil.RefreshClaims valid = new JwtUtil.RefreshClaims(42L, "session-1", "token-id-1");
        JwtUtil.RefreshClaims wrongToken = new JwtUtil.RefreshClaims(42L, "session-1", "token-id-2");

        assertTrue(service.isActive(valid));
        assertFalse(service.isActive(wrongToken));
    }

    @Test
    void revokeDeletesSessionAndUserIndexEntry() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        JwtUtil.RefreshClaims claims = new JwtUtil.RefreshClaims(42L, "session-1", "token-id-1");

        service.revoke(claims);

        verify(redisTemplate).delete("user-service:refresh-session:session-1");
        verify(setOperations).remove("user-service:refresh-sessions:user:42", "session-1");
    }
}
