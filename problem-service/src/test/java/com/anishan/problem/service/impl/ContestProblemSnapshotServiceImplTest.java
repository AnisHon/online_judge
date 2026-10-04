package com.anishan.problem.service.impl;

import com.anishan.commons.enumeration.ProblemType;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.commons.exception.BusinessException;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.ContestProblemSnapshot;
import com.anishan.problem.domain.entity.ContestRankSnapshot;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.domain.entity.ProblemProblemListRelation;
import com.anishan.problem.domain.enumeration.ContestRankState;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestProblemSnapshotMapper;
import com.anishan.problem.mapper.ContestRankSnapshotMapper;
import com.anishan.problem.mapper.ProblemListMapper;
import com.anishan.problem.mapper.ProblemMapper;
import com.anishan.problem.mapper.ProblemProblemListMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContestProblemSnapshotServiceImplTest {
    private final ContestMapper contestMapper = mock(ContestMapper.class);
    private final ProblemListMapper problemListMapper = mock(ProblemListMapper.class);
    private final ProblemProblemListMapper relationMapper = mock(ProblemProblemListMapper.class);
    private final ProblemMapper problemMapper = mock(ProblemMapper.class);
    private final ContestProblemSnapshotMapper snapshotMapper = mock(ContestProblemSnapshotMapper.class);
    private final ContestRankSnapshotMapper rankSnapshotMapper = mock(ContestRankSnapshotMapper.class);
    private final ContestAttemptMapper attemptMapper = mock(ContestAttemptMapper.class);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private final ContestProblemSnapshotServiceImpl service = new ContestProblemSnapshotServiceImpl(
            contestMapper, problemListMapper, relationMapper, problemMapper, snapshotMapper,
            rankSnapshotMapper, attemptMapper, CLOCK);

    private Contest contest;

    @BeforeEach
    void setUp() {
        contest = new Contest();
        contest.setContestId(900L);
        contest.setListId(80L);
        contest.setProblemSnapshotAt(null);
        when(contestMapper.selectContestForUpdate(900L)).thenReturn(contest);
        when(problemListMapper.selectUpdateTimeForUpdate(80L)).thenReturn(LocalDateTime.of(2026, 10, 3, 12, 0));
        when(snapshotMapper.selectByContestId(900L)).thenReturn(Collections.emptyList());
        when(snapshotMapper.insert(any(ContestProblemSnapshot.class))).thenReturn(1);
        when(contestMapper.setProblemSnapshotAt(eq(900L), any(LocalDateTime.class))).thenReturn(1);
    }

    @Test
    void createsStableNormalizedOrderAndLocksContestListThenSortedProblems() {
        ProblemProblemListRelation second = relation(200L, 2, "20");
        ProblemProblemListRelation first = relation(100L, 2, "10");
        when(relationMapper.selectList(any())).thenReturn(new ArrayList<>(Arrays.asList(second, first)));
        when(problemMapper.selectByProblemIdsForUpdate(Arrays.asList(100L, 200L)))
                .thenReturn(Arrays.asList(problem(200L), problem(100L)));

        List<ContestProblemSnapshot> rows = service.createIfAbsent(900L);

        assertEquals(2, rows.size());
        assertEquals(100L, rows.get(0).getProblemId());
        assertEquals(1, rows.get(0).getProblemOrder());
        assertEquals(new BigDecimal("10"), rows.get(0).getMaxScore());
        assertEquals(200L, rows.get(1).getProblemId());
        assertEquals(2, rows.get(1).getProblemOrder());
        assertEquals(1, rows.get(0).getProblemType());

        InOrder lockOrder = inOrder(contestMapper, snapshotMapper, problemListMapper, relationMapper, problemMapper);
        lockOrder.verify(contestMapper).selectContestForUpdate(900L);
        lockOrder.verify(snapshotMapper).selectByContestId(900L);
        lockOrder.verify(problemListMapper).selectUpdateTimeForUpdate(80L);
        lockOrder.verify(relationMapper).selectList(any());
        lockOrder.verify(problemMapper).selectByProblemIdsForUpdate(Arrays.asList(100L, 200L));
    }

    @Test
    void repeatedCreateReturnsPersistedSnapshotWithoutRebuildingFromLiveList() {
        ContestProblemSnapshot persisted = new ContestProblemSnapshot();
        persisted.setContestId(900L);
        persisted.setProblemId(123L);
        when(snapshotMapper.selectByContestId(900L)).thenReturn(Collections.singletonList(persisted));

        List<ContestProblemSnapshot> rows = service.createIfAbsent(900L);

        assertEquals(Collections.singletonList(persisted), rows);
        verify(problemListMapper, never()).selectUpdateTimeForUpdate(any());
        verify(relationMapper, never()).selectList(any());
        verify(problemMapper, never()).selectByProblemIdsForUpdate(anyList());
        verify(snapshotMapper, never()).insert(any(ContestProblemSnapshot.class));
    }

    @Test
    void firstAuthorizedReadDoesNotFreezeTheRosterBeforeStart() {
        contest.setStartTime(LocalDateTime.of(2026, 10, 3, 21, 0));

        ApiStatusException error = assertThrows(ApiStatusException.class,
                () -> service.createAfterStart(900L));

        assertEquals(409, error.getStatusCode());
        verify(problemListMapper, never()).selectUpdateTimeForUpdate(any());
        verify(relationMapper, never()).selectList(any());
    }

    @Test
    void refusesDeletedProblemOrNegativeScoreWithoutPersistingPartialSnapshot() {
        when(relationMapper.selectList(any())).thenReturn(Collections.singletonList(relation(100L, 1, "-1")));
        when(problemMapper.selectByProblemIdsForUpdate(Collections.singletonList(100L)))
                .thenReturn(Collections.singletonList(problem(100L)));

        assertThrows(BusinessException.class, () -> service.createIfAbsent(900L));
        verify(snapshotMapper, never()).insert(any(ContestProblemSnapshot.class));
        verify(contestMapper, never()).setProblemSnapshotAt(eq(900L), any(LocalDateTime.class));
    }

    @Test
    void resetIsRejectedWhenFormalAttemptExists() {
        when(attemptMapper.countByContestId(900L)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.resetIfNoAttempts(900L, 81L));
        verify(problemListMapper, never()).selectUpdateTimeForUpdate(any());
        verify(snapshotMapper, never()).deleteByContestId(900L);
    }

    @Test
    void resetIsRejectedAfterRankPublicationEvenForAnEmptyAttemptSet() {
        ContestRankSnapshot rankSnapshot = new ContestRankSnapshot();
        rankSnapshot.setState(ContestRankState.READY);
        rankSnapshot.setVersion(1L);
        when(rankSnapshotMapper.selectByContestId(900L)).thenReturn(rankSnapshot);

        assertThrows(BusinessException.class, () -> service.resetIfNoAttempts(900L, 81L));
        verify(problemListMapper, never()).selectUpdateTimeForUpdate(any());
        verify(snapshotMapper, never()).deleteByContestId(900L);
    }

    private ProblemProblemListRelation relation(Long problemId, Integer order, String score) {
        ProblemProblemListRelation relation = new ProblemProblemListRelation();
        relation.setListId(80L);
        relation.setProblemId(problemId);
        relation.setProblemOrder(order);
        relation.setScore(new BigDecimal(score));
        return relation;
    }

    private Problem problem(Long problemId) {
        Problem problem = new Problem();
        problem.setProblemId(problemId);
        problem.setTitle("Problem " + problemId);
        problem.setType(ProblemType.OJ);
        problem.setDelFlag(0);
        return problem;
    }
}
