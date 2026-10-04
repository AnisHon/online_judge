package com.anishan.problem.service.impl;

import com.anishan.commons.enumeration.ContestType;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.config.ContestRankProperties;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.ContestRankSnapshot;
import com.anishan.problem.domain.enumeration.ContestRankState;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestRankEntryMapper;
import com.anishan.problem.mapper.ContestRankSnapshotMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContestFinalRankServiceTest {

    @Test
    void retryBackoffUsesTheDocumentedBoundedExponentialSchedule() {
        assertEquals(15L, ContestFinalRankServiceImpl.retryDelaySeconds(1));
        assertEquals(30L, ContestFinalRankServiceImpl.retryDelaySeconds(2));
        assertEquals(60L, ContestFinalRankServiceImpl.retryDelaySeconds(3));
        assertEquals(120L, ContestFinalRankServiceImpl.retryDelaySeconds(4));
        assertEquals(240L, ContestFinalRankServiceImpl.retryDelaySeconds(5));
        assertEquals(300L, ContestFinalRankServiceImpl.retryDelaySeconds(6));
        assertEquals(300L, ContestFinalRankServiceImpl.retryDelaySeconds(20));
    }

    @Test
    void newContestHeaderHasExplicitCurrentRuleAndDueTime() {
        Fixture fixture = new Fixture();
        LocalDateTime now = LocalDateTime.of(2026, 10, 4, 10, 0);
        when(fixture.contests.selectContestForUpdate(11L)).thenReturn(contest(11L, 2, now.plusDays(1)));

        fixture.service.initializeForContest(11L);

        org.mockito.ArgumentCaptor<ContestRankSnapshot> captor =
                org.mockito.ArgumentCaptor.forClass(ContestRankSnapshot.class);
        verify(fixture.snapshots).insertInitial(captor.capture());
        assertEquals(ContestRankState.WAITING, captor.getValue().getState());
        assertEquals(0L, captor.getValue().getVersion());
        assertEquals(0L, captor.getValue().getNextVersion());
        assertEquals(now, captor.getValue().getNextBuildAt());
        assertEquals("CURRENT", captor.getValue().getSourceMode());
        assertEquals("WEIGHTED_LATEST_V1", captor.getValue().getRuleVersion());
    }

    @Test
    void homeworkDoesNotGetARankHeader() {
        Fixture fixture = new Fixture();
        LocalDateTime now = LocalDateTime.of(2026, 10, 4, 10, 0);
        Contest homework = contest(12L, 2, now.plusDays(1));
        homework.setType(ContestType.HOMEWORK);
        when(fixture.contests.selectContestForUpdate(12L)).thenReturn(homework);

        fixture.service.initializeForContest(12L);

        verify(fixture.snapshots, never()).insertInitial(any());
    }

    @Test
    void rebuildRequestDoesNotDisplaceAnActiveLease() {
        Fixture fixture = new Fixture();
        LocalDateTime now = LocalDateTime.of(2026, 10, 4, 10, 0);
        when(fixture.contests.selectContestForUpdate(13L)).thenReturn(contest(13L, 2, now.minusMinutes(1)));
        when(fixture.snapshots.selectByContestIdForUpdate(13L)).thenReturn(header(13L,
                ContestRankState.BUILDING, now.plusSeconds(20)));
        when(fixture.attempts.countPendingByContestId(13L)).thenReturn(0L);
        when(fixture.attempts.selectInconsistentAttemptReferences(13L, 8)).thenReturn(Collections.emptyList());
        when(fixture.attempts.selectUnlinkedUnappliedSubmitIds(13L, 8)).thenReturn(Collections.emptyList());

        assertFalse(fixture.service.requestRebuild(13L));

        verify(fixture.snapshots, never()).scheduleRebuild(eq(13L), any());
    }

    @Test
    void rebuildIsRejectedWhileJudgeResultsArePending() {
        Fixture fixture = new Fixture();
        LocalDateTime now = LocalDateTime.of(2026, 10, 4, 10, 0);
        when(fixture.contests.selectContestForUpdate(14L)).thenReturn(contest(14L, 2, now.minusMinutes(1)));
        when(fixture.snapshots.selectByContestIdForUpdate(14L)).thenReturn(header(14L,
                ContestRankState.READY, now.minusMinutes(1)));
        when(fixture.attempts.countPendingByContestId(14L)).thenReturn(1L);
        when(fixture.attempts.selectInconsistentAttemptReferences(14L, 8)).thenReturn(Collections.emptyList());
        when(fixture.attempts.selectUnlinkedUnappliedSubmitIds(14L, 8)).thenReturn(Collections.emptyList());

        ApiStatusException error = org.junit.jupiter.api.Assertions.assertThrows(ApiStatusException.class,
                () -> fixture.service.requestRebuild(14L));

        assertEquals(409, error.getStatusCode());
        verify(fixture.snapshots, never()).scheduleRebuild(eq(14L), any());
    }

    private Contest contest(Long id, int scoringVersion, LocalDateTime endTime) {
        Contest contest = new Contest();
        contest.setContestId(id);
        contest.setType(ContestType.CONTEST);
        contest.setDelFlag(0);
        contest.setScoringVersion(scoringVersion);
        contest.setNextAttemptSeq(0L);
        contest.setEndTime(endTime);
        return contest;
    }

    private ContestRankSnapshot header(Long id, ContestRankState state, LocalDateTime leaseUntil) {
        ContestRankSnapshot snapshot = new ContestRankSnapshot();
        snapshot.setContestId(id);
        snapshot.setState(state);
        snapshot.setVersion(1L);
        snapshot.setNextVersion(1L);
        snapshot.setNextBuildAt(LocalDateTime.of(2026, 10, 4, 9, 0));
        snapshot.setLeaseUntil(leaseUntil);
        snapshot.setLeaseOwner("active-owner");
        snapshot.setSourceMode("CURRENT");
        snapshot.setRuleVersion("WEIGHTED_LATEST_V1");
        snapshot.setBuildAttempts(0);
        return snapshot;
    }

    private static final class Fixture {
        private final ContestMapper contests = mock(ContestMapper.class);
        private final ContestAttemptMapper attempts = mock(ContestAttemptMapper.class);
        private final ContestRankSnapshotMapper snapshots = mock(ContestRankSnapshotMapper.class);
        private final ContestRankEntryMapper entries = mock(ContestRankEntryMapper.class);
        private final ContestRankProperties properties = new ContestRankProperties();
        private final ContestFinalRankServiceImpl service = new ContestFinalRankServiceImpl(
                contests, attempts, snapshots, entries, properties,
                Clock.fixed(Instant.parse("2026-10-04T02:00:00Z"), ZoneId.of("Asia/Shanghai")));
    }
}
