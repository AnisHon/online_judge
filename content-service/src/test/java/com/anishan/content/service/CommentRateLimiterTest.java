package com.anishan.content.service;

import com.anishan.commons.exception.ApiStatusException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommentRateLimiterTest {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void runsOneAtomicTenPerMinuteScriptWithAnExpiringCounter() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), any(), any())).thenReturn(10L);
        CommentRateLimiter limiter = new CommentRateLimiter(redis);

        assertDoesNotThrow(() -> limiter.acquire(42L));
        ArgumentCaptor<DefaultRedisScript> scriptCaptor = ArgumentCaptor.forClass(DefaultRedisScript.class);
        ArgumentCaptor<List> keys = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Object> max = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> window = ArgumentCaptor.forClass(Object.class);
        verify(redis).execute(scriptCaptor.capture(), keys.capture(), max.capture(), window.capture());
        assertSame(CommentRateLimiter.LIMIT_SCRIPT, scriptCaptor.getValue());
        assertEquals(List.of("content:comment:write:42"), keys.getValue());
        assertEquals("10", max.getValue());
        assertEquals("60", window.getValue());
        String script = CommentRateLimiter.LIMIT_SCRIPT.getScriptAsString();
        assertTrue(script.contains("redis.call('INCR'"));
        assertTrue(script.contains("redis.call('EXPIRE', KEYS[1], ARGV[2])"));
        assertTrue(script.contains("redis.call('TTL', KEYS[1]) < 0"));
        assertTrue(script.contains("count > tonumber(ARGV[1])"));
        assertTrue(com.anishan.content.service.CacheCatalog.isManagedPrefix("content:comment:write:"));
        assertTrue(com.anishan.content.service.CacheCatalog.types().stream()
                .anyMatch(type -> type.getId() == 24 && "content:comment:write:".equals(type.getType())));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void rejectsEleventhAttemptAndFailsClosedWhenRedisIsUnavailable() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), any(), any())).thenReturn(0L);
        CommentRateLimiter limiter = new CommentRateLimiter(redis);
        ApiStatusException limited = assertThrows(ApiStatusException.class, () -> limiter.acquire(42L));
        assertEquals(429, limited.getStatusCode());

        when(redis.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                .thenThrow(new IllegalStateException("redis unavailable"));
        ApiStatusException unavailable = assertThrows(ApiStatusException.class, () -> limiter.acquire(42L));
        assertEquals(503, unavailable.getStatusCode());
        assertFalse(unavailable.getMessage().contains("redis unavailable"));
    }
}
