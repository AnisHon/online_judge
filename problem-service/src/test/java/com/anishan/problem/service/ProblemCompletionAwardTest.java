package com.anishan.problem.service;

import com.anishan.api.event.PointAwardEvent;
import com.anishan.problem.config.JudgeConfig;
import com.anishan.problem.mapper.ProblemCompleteMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProblemCompletionAwardTest {

    @Test
    void onlyFirstCompletionCreatesAnExactDecimalPointsEvent() {
        ProblemCompleteMapper completions = mock(ProblemCompleteMapper.class);
        ProblemEventOutboxService outbox = mock(ProblemEventOutboxService.class);
        JudgeConfig config = new JudgeConfig();
        config.setAwardPoint("2.345");
        ProblemCompletionAwardService service = new ProblemCompletionAwardService(completions, outbox,
                config, Clock.fixed(Instant.parse("2026-10-04T00:00:00Z"), ZoneOffset.UTC));
        when(completions.insertIgnore(7L, 99L)).thenReturn(1, 0);

        assertTrue(service.completeOnAc(7L, 99L, new BigDecimal("8.00")));
        assertFalse(service.completeOnAc(7L, 99L, new BigDecimal("8.00")));

        ArgumentCaptor<PointAwardEvent> event = ArgumentCaptor.forClass(PointAwardEvent.class);
        verify(outbox, times(1)).recordPointAward(event.capture());
        assertEquals(1, event.getValue().getSchemaVersion());
        assertEquals("7", event.getValue().getUserId());
        assertEquals("99", event.getValue().getProblemId());
        assertEquals("2.34", event.getValue().getAmount());
        assertEquals("points-awarded:7:99", event.getValue().getDedupeKey());
        assertTrue(event.getValue().getEventId() != null);
    }

    @Test
    void virtualUserGetsCompletionMarkerButNoPointsEvent() {
        ProblemCompleteMapper completions = mock(ProblemCompleteMapper.class);
        ProblemEventOutboxService outbox = mock(ProblemEventOutboxService.class);
        when(completions.insertIgnore(0L, 99L)).thenReturn(1);
        ProblemCompletionAwardService service = new ProblemCompletionAwardService(completions, outbox,
                new JudgeConfig(), Clock.systemUTC());

        assertTrue(service.completeOnAc(0L, 99L, new BigDecimal("10.00")));
        verify(outbox, never()).recordPointAward(any(PointAwardEvent.class));
    }

    @Test
    void zeroScoreAcStillCreatesCompletionMarkerWithoutPointsEvent() {
        ProblemCompleteMapper completions = mock(ProblemCompleteMapper.class);
        ProblemEventOutboxService outbox = mock(ProblemEventOutboxService.class);
        when(completions.insertIgnore(7L, 99L)).thenReturn(1);
        JudgeConfig config = new JudgeConfig();
        config.setIsFixedAwardPoint(false);
        ProblemCompletionAwardService service = new ProblemCompletionAwardService(completions, outbox,
                config, Clock.systemUTC());

        assertTrue(service.completeOnAc(7L, 99L, BigDecimal.ZERO));
        verify(completions).insertIgnore(7L, 99L);
        verify(outbox, never()).recordPointAward(any(PointAwardEvent.class));
    }
}
