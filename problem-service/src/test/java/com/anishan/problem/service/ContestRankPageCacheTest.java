package com.anishan.problem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContestRankPageCacheTest {

    private static final Long CONTEST_ID = 9007199254740997L;
    private static final Long USER_ID = 9007199254740993L;

    @SuppressWarnings("unchecked")
    @Test
    void usesVersionedKeySixHourTtlAndPlainJsonForOnlyTheRankPage() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        ContestRankPageCache cache = new ContestRankPageCache(redis, new ObjectMapper());
        String key = cache.key(CONTEST_ID, 8L, 2L, 50);
        assertEquals("problem:contest:final-rank:v1:9007199254740997:8:2:50", key);

        ContestRankPageCache.PageSnapshot page = page(8L);
        page.setPage(2L);
        page.setSize(50);
        page.setTotalRecords(51L);
        page.getEntries().get(0).setRank(51L);
        page.getEntries().get(0).setRowPosition(51L);
        cache.put(key, page);

        org.mockito.ArgumentCaptor<String> serialized = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(values).set(eq(key), serialized.capture(), eq(Duration.ofHours(6)));
        assertFalse(serialized.getValue().contains("@class"));
        assertFalse(serialized.getValue().contains("email"));
        when(values.get(key)).thenReturn(serialized.getValue());
        ContestRankPageCache.PageSnapshot restored = cache.get(key, CONTEST_ID, 8L, 2L, 50, 51L);
        assertNotNull(restored, serialized.getValue());
        assertEquals(USER_ID, restored.getEntries().get(0).getUserId());
        assertEquals("12.25", restored.getEntries().get(0).getScore());
        verify(redis, never()).delete(anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    void malformedOrMismatchedEntryDeletesOnlyThatKeyAndFallsBackAsCacheMiss() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        ContestRankPageCache cache = new ContestRankPageCache(redis, new ObjectMapper());
        String key = cache.key(CONTEST_ID, 8L, 1L, 20);
        when(values.get(key)).thenReturn("{not-json");

        assertNull(cache.get(key, CONTEST_ID, 8L, 1L, 20, 1L));
        verify(redis).delete(key);

        ContestRankPageCache.PageSnapshot wrongVersion = page(7L);
        when(values.get(key)).thenReturn(new ObjectMapper().writeValueAsString(wrongVersion));
        assertNull(cache.get(key, CONTEST_ID, 8L, 1L, 20, 1L));
        verify(redis, org.mockito.Mockito.times(2)).delete(key);
    }

    @SuppressWarnings("unchecked")
    @Test
    void redisOutageIsAnOrdinaryMissAndWriteFailureDoesNotFailTheRequest() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new IllegalStateException("test-only outage"));
        ContestRankPageCache cache = new ContestRankPageCache(redis, new ObjectMapper());
        String key = cache.key(CONTEST_ID, 8L, 1L, 20);

        assertNull(cache.get(key, CONTEST_ID, 8L, 1L, 20, 1L));
        cache.put(key, page(8L));
        assertEquals(Duration.ofHours(6), ContestRankPageCache.TTL);
    }

    private ContestRankPageCache.PageSnapshot page(long version) {
        ContestRankPageCache.PageSnapshot page = new ContestRankPageCache.PageSnapshot();
        page.setContestId(CONTEST_ID);
        page.setVersion(version);
        page.setPage(1L);
        page.setSize(20);
        page.setTotalRecords(1L);
        ContestRankPageCache.CachedRankEntry entry = new ContestRankPageCache.CachedRankEntry();
        entry.setRank(1L);
        entry.setRowPosition(1L);
        entry.setUserId(USER_ID);
        entry.setScore("12.25");
        entry.setCorrectCount(1);
        entry.setAnsweredCount(2);
        entry.setHandedIn(Boolean.TRUE);
        page.setEntries(Collections.singletonList(entry));
        return page;
    }
}
