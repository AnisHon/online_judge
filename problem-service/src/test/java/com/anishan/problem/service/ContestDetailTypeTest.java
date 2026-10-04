package com.anishan.problem.service;

import com.anishan.api.util.CacheUtil;
import com.anishan.commons.enumeration.ContestType;
import com.anishan.problem.controller.ContestController;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.vo.ContestVo;
import com.anishan.problem.service.impl.ContestServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContestDetailTypeTest {
    @Test
    void detailSelectIncludesActivityTypeButNotPassword() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), Contest.class);
        ContestServiceImpl service = mock(ContestServiceImpl.class, CALLS_REAL_METHODS);
        Contest row = new Contest();
        row.setContestId(2098755579163770881L);
        row.setType(ContestType.CONTEST);
        doAnswer(invocation -> {
            LambdaQueryWrapper<Contest> query = invocation.getArgument(0);
            assertTrue(java.util.Arrays.asList(query.getSqlSelect().split(",")).contains("type"));
            assertFalse(query.getSqlSelect().contains("password"));
            return row;
        }).when(service).getOne(any());
        assertEquals(ContestType.CONTEST, service.getContestById(row.getContestId()).getType());
    }

    @Test
    void olderCachedDetailsAreEvictedIndividuallyBeforeRetry() {
        CacheManager manager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        when(manager.getCache("problem:contest:")).thenReturn(cache);
        new CacheUtil(manager);
        ContestService service = mock(ContestService.class);
        ContestVo stale = new ContestVo(), fresh = new ContestVo();
        fresh.setType(ContestType.HOMEWORK);
        when(service.getContestById(42L)).thenReturn(stale, fresh);
        ContestController controller = new ContestController(service, mock(ContestParticipationService.class));
        controller.getContestById(42L);
        verify(cache).evict(42L);
        verify(cache, never()).clear();
        verify(service, times(2)).getContestById(42L);
    }

    @Test
    void completeDetailsDoNotEvictOrRepeatQueries() {
        ContestService service = mock(ContestService.class);
        ContestVo fresh = new ContestVo(); fresh.setType(ContestType.CONTEST);
        when(service.getContestById(42L)).thenReturn(fresh);
        new ContestController(service, mock(ContestParticipationService.class)).getContestById(42L);
        verify(service).getContestById(42L);
    }
}
