package com.anishan.problem.service.impl;

import com.anishan.problem.domain.vo.ProfileActivityDayVo;
import com.anishan.problem.domain.vo.ProfileActivityHeatmapVo;
import com.anishan.problem.mapper.ProfileMapper;
import com.anishan.problem.service.ProfileActivityCacheService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileServiceImplTest {
    @Test
    void fillsZerosAndUsesInclusiveYearWithExclusiveNextMidnight() {
        ProfileMapper mapper = mock(ProfileMapper.class);
        LocalDate today = LocalDate.of(2026, 10, 3);
        LocalDate start = LocalDate.of(2025, 10, 4);
        ProfileActivityDayVo first = day(start.toString(), 3);
        ProfileActivityDayVo last = day(today.toString(), 12);
        when(mapper.selectActivityDays(42L, start.atStartOfDay(), today.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(first, last, day("2025-10-03", 999), day("2026-10-04", 999)));

        ProfileActivityHeatmapVo heatmap = new ProfileActivityCacheService(mapper).buildHeatmap(42L, today);

        assertEquals("Asia/Shanghai", heatmap.getTimeZone());
        assertEquals(start.toString(), heatmap.getStartDate());
        assertEquals(today.toString(), heatmap.getEndDate());
        assertEquals(365, heatmap.getDays().size());
        assertEquals(3, heatmap.getDays().get(0).getCount());
        assertEquals(0, heatmap.getDays().get(1).getCount());
        assertEquals(12, heatmap.getDays().get(364).getCount());
        assertEquals(15, heatmap.getDays().stream().mapToLong(ProfileActivityDayVo::getCount).sum());
        verify(mapper).selectActivityDays(42L, start.atStartOfDay(), today.plusDays(1).atStartOfDay());
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void leapYearNeverExceeds366DaysAndHandlesEmptyHistory() {
        ProfileMapper mapper = mock(ProfileMapper.class);
        LocalDate today = LocalDate.of(2024, 2, 29);
        LocalDate start = LocalDate.of(2023, 3, 1);
        when(mapper.selectActivityDays(42L, start.atStartOfDay(), today.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());

        ProfileActivityHeatmapVo heatmap = new ProfileActivityCacheService(mapper).buildHeatmap(42L, today);

        assertEquals(366, heatmap.getDays().size());
        assertEquals("2024-02-29", heatmap.getDays().get(365).getDate());
        assertTrue(heatmap.getDays().stream().allMatch(day -> day.getCount() == 0));
    }

    private ProfileActivityDayVo day(String date, long count) {
        ProfileActivityDayVo day = new ProfileActivityDayVo();
        day.setDate(date);
        day.setCount(count);
        return day;
    }
}
