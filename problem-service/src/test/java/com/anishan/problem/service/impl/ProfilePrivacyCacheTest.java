package com.anishan.problem.service.impl;

import com.anishan.api.annotation.EnableCache;
import com.anishan.problem.domain.vo.*;
import com.anishan.problem.service.ProfileActivityCacheService;
import com.anishan.problem.mapper.ProfileMapper;
import com.anishan.api.aspect.CacheAspect;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfilePrivacyCacheTest {
    @Test
    @SuppressWarnings("unchecked")
    void independentBeanActuallyHitsV3CacheWithoutOwnerInTheKey() {
        ProfileMapper mapper = mock(ProfileMapper.class);
        when(mapper.selectSolvedProblems(anyLong(), anyInt())).thenReturn(List.of());
        when(mapper.selectContests(anyLong(), anyInt())).thenReturn(List.of());
        when(mapper.selectActivityDays(anyLong(), any(), any())).thenReturn(List.of());
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        ProfileActivityBaseVo hot = new ProfileActivityBaseVo();
        hot.setUserId(42L); hot.setSolvedCount(7);
        when(values.get(anyString())).thenReturn(null, hot);
        AspectJProxyFactory factory = new AspectJProxyFactory(new ProfileActivityCacheService(mapper));
        factory.setProxyTargetClass(true);
        factory.addAspect(new CacheAspect(redis));
        ProfileActivityCacheService proxy = factory.getProxy();
        ProfileServiceImpl service = new ProfileServiceImpl(proxy);
        assertTrue(service.getActivity(42L, true).isOwner());
        var visitor = service.getActivity(42L, false);
        assertFalse(visitor.isOwner());
        assertEquals(7, visitor.getSolvedCount());
        verify(mapper, times(1)).countSolved(42L);
        verify(values).set(startsWith("problem:profile:v3:"), isA(ProfileActivityBaseVo.class),
                eq(7200000L), eq(TimeUnit.MILLISECONDS));
        verify(values, times(2)).get(startsWith("problem:profile:v3:"));
    }

    @Test
    void cacheIsViewerIndependentAndHotInstanceCannotBePolluted() throws Exception {
        var annotation = ProfileActivityCacheService.class.getMethod("getBase", Long.class)
                .getAnnotation(EnableCache.class);
        assertEquals("problem:profile:v3", annotation.name());
        assertEquals(7200000, annotation.expire());
        ProfileActivityBaseVo base = new ProfileActivityBaseVo();
        base.setUserId(2098755579163770881L);
        base.setSolvedProblems(new LinkedHashMap<>(Map.of("easy", new ArrayList<>(List.of("42")))));
        ProfileContestVo contest = new ProfileContestVo();
        contest.setTitle("比赛");
        base.setContests(new ArrayList<>(List.of(contest)));
        ProfileActivityHeatmapVo heatmap = new ProfileActivityHeatmapVo();
        ProfileActivityDayVo day = new ProfileActivityDayVo();
        day.setCount(1); heatmap.setDays(new ArrayList<>(List.of(day))); base.setHeatmap(heatmap);
        var cache = mock(ProfileActivityCacheService.class);
        when(cache.getBase(base.getUserId())).thenReturn(base);
        var service = new ProfileServiceImpl(cache);
        var own = service.getActivity(base.getUserId(), true);
        own.getSolvedProblems().get("easy").clear();
        own.getContests().get(0).setTitle("污染");
        own.getHeatmap().getDays().get(0).setCount(999);
        var visitor = service.getActivity(base.getUserId(), false);
        assertTrue(own.isOwner()); assertFalse(visitor.isOwner());
        assertEquals(List.of("42"), visitor.getSolvedProblems().get("easy"));
        assertEquals("比赛", visitor.getContests().get(0).getTitle());
        assertEquals(1, visitor.getHeatmap().getDays().get(0).getCount());
        var json = new ObjectMapper().writeValueAsString(base);
        assertFalse(json.contains("owner")); assertFalse(json.contains("solutions"));
        assertTrue(json.contains("\"userId\":\"2098755579163770881\""));
        assertFalse(new ObjectMapper().writeValueAsString(visitor).contains("solutions"));
        assertNull(ProfileServiceImpl.class.getMethod("getActivity", Long.class, boolean.class)
                .getAnnotation(EnableCache.class));
    }
}
