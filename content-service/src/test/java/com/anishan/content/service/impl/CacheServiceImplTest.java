package com.anishan.content.service.impl;

import com.anishan.content.domain.vo.CacheKeyPage;
import com.anishan.content.domain.vo.CacheVo;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CacheServiceImplTest {

    @Test
    void listsKeysUsingSpringRedisTemplateAndSimpleOffsetPages() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.keys("problem:detail:*")).thenReturn(new HashSet<>(Arrays.asList(
                "problem:detail:3", "problem:detail:1", "problem:detail:2"
        )));
        CacheServiceImpl service = new CacheServiceImpl(redis);

        CacheKeyPage first = service.list("problem:detail:", "0", 2);
        CacheKeyPage second = service.list("problem:detail:", first.getNextCursor(), 2);

        assertEquals(Arrays.asList("problem:detail:1", "problem:detail:2"), first.getKeys());
        assertEquals("2", first.getNextCursor());
        assertTrue(first.isHasMore());
        assertEquals(Arrays.asList("problem:detail:3"), second.getKeys());
        assertEquals("0", second.getNextCursor());
        assertFalse(second.isHasMore());
    }

    @Test
    void readsAndRemovesKeysWithoutPrefixWhitelistErrors() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.type("custom:legacy-key")).thenReturn(DataType.NONE);
        when(redis.getExpire("custom:legacy-key", java.util.concurrent.TimeUnit.SECONDS)).thenReturn(-2L);
        when(redis.delete("custom:legacy-key")).thenReturn(true);
        CacheServiceImpl service = new CacheServiceImpl(redis);

        CacheVo value = service.get("custom:legacy-key");

        assertEquals("custom:legacy-key", value.getKey());
        assertEquals(-2L, value.getExpireTime());
        assertTrue(service.remove("custom:legacy-key"));
    }
}
