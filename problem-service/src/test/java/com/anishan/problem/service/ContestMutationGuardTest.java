package com.anishan.problem.service;

import com.anishan.api.domain.entity.OjProblemCase;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestMutationReferenceMapper;
import com.anishan.problem.mapper.OjProblemCaseMapper;
import com.anishan.problem.mapper.ProblemMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContestMutationGuardTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private final ContestMutationReferenceMapper references = mock(ContestMutationReferenceMapper.class);
    private final ContestMapper contests = mock(ContestMapper.class);
    private final ProblemMapper problems = mock(ProblemMapper.class);
    private final OjProblemCaseMapper cases = mock(OjProblemCaseMapper.class);
    private final ContestMutationGuard guard = new ContestMutationGuard(references, contests, problems, cases, CLOCK);

    @BeforeEach
    void setUp() {
        when(references.selectBusyContestIds(any(), any())).thenReturn(Collections.emptyList());
        when(references.selectBusySnapshotContestIdsCurrent(any(), any())).thenReturn(Collections.emptyList());
        when(references.selectBusyListContestIdsCurrent(any(), any())).thenReturn(Collections.emptyList());
        when(references.selectPendingContestIdsCurrent(any())).thenReturn(Collections.emptyList());
        when(problems.selectByProblemIdsForUpdate(any())).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            return ids.stream().map(id -> problem(id)).collect(java.util.stream.Collectors.toList());
        });
        when(cases.selectList(org.mockito.ArgumentMatchers.<Wrapper<OjProblemCase>>any()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void locksActivitiesInAscendingOrderThenProblemsAndCases() {
        when(references.selectBusyContestIds(eq(Collections.singletonList(10L)), any()))
                .thenReturn(Arrays.asList(30L, 20L));
        when(contests.selectContestForUpdate(20L)).thenReturn(new Contest());
        when(contests.selectContestForUpdate(30L)).thenReturn(new Contest());

        ContestMutationGuard.MutationContext context = guard.lockAndInspect(Collections.singletonList(10L));

        assertEquals(Collections.singletonList(10L), context.getProblemIds());
        InOrder locks = inOrder(contests, problems, cases);
        locks.verify(contests).selectContestForUpdate(20L);
        locks.verify(contests).selectContestForUpdate(30L);
        locks.verify(problems).selectByProblemIdsForUpdate(Collections.singletonList(10L));
        locks.verify(cases).selectList(org.mockito.ArgumentMatchers.<Wrapper<OjProblemCase>>any());
    }

    @Test
    void rejectsActiveSnapshotAndPendingReferencesButAllowsIndependentProblems() {
        when(references.selectBusyContestIds(eq(Collections.singletonList(10L)), any()))
                .thenReturn(Collections.singletonList(20L));
        when(contests.selectContestForUpdate(20L)).thenReturn(new Contest());
        when(references.selectBusySnapshotContestIdsCurrent(eq(Collections.singletonList(10L)), any()))
                .thenReturn(Collections.singletonList(20L));

        ContestMutationGuard.MutationContext busy = guard.lockAndInspect(Collections.singletonList(10L));
        ApiStatusException rejected = assertThrows(ApiStatusException.class,
                () -> guard.requireScoringMutable(busy));
        assertEquals(409, rejected.getStatusCode());

        when(references.selectBusyContestIds(eq(Collections.singletonList(11L)), any()))
                .thenReturn(Collections.emptyList());
        ContestMutationGuard.MutationContext independent = guard.lockAndInspect(Collections.singletonList(11L));
        assertDoesNotThrow(() -> guard.requireScoringMutable(independent));
    }

    @Test
    void detectsAContestReferenceCreatedAfterTheInitialReadWithoutReverseLockingIt() {
        when(references.selectBusyListContestIdsCurrent(eq(Collections.singletonList(10L)), any()))
                .thenReturn(Collections.singletonList(99L));

        ApiStatusException rejected = assertThrows(ApiStatusException.class,
                () -> guard.lockAndInspect(Collections.singletonList(10L)));

        assertEquals(409, rejected.getStatusCode());
        org.mockito.Mockito.verify(contests, org.mockito.Mockito.never()).selectContestForUpdate(99L);
    }

    @Test
    void normalizesAndLocksBatchProblemIdsInAscendingOrder() {
        guard.lockAndInspect(Arrays.asList(30L, 10L, 30L));
        org.mockito.Mockito.verify(problems).selectByProblemIdsForUpdate(Arrays.asList(10L, 30L));
    }

    private static Problem problem(Long id) {
        Problem problem = new Problem();
        problem.setProblemId(id);
        problem.setDelFlag(0);
        return problem;
    }
}
