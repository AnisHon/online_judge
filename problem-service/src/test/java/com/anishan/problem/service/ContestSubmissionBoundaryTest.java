package com.anishan.problem.service;

import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.commons.enumeration.ContestType;
import com.anishan.commons.enumeration.ProblemType;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.ContestSubmissionContext;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.ContestAttempt;
import com.anishan.problem.domain.entity.ContestProblemSnapshot;
import com.anishan.problem.domain.entity.SupplementContest;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ProblemEventOutboxMapper;
import com.anishan.problem.mapper.SupplementContestMapper;
import com.anishan.problem.mapper.SubmitLogMapper;
import com.anishan.problem.mapper.UserContestMapper;
import com.anishan.problem.mapper.UserSubmitMapper;
import com.anishan.problem.domain.vo.ProblemJudgeResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ContestSubmissionBoundaryTest {

    private static final Long CONTEST_ID = 300L;
    private static final Long USER_ID = 100L;
    private static final Long PROBLEM_ID = 200L;
    private static final Long SUBMIT_ID = 2098931802871431170L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    private final ContestMapper contestMapper = mock(ContestMapper.class);
    private final UserContestMapper userContestMapper = mock(UserContestMapper.class);
    private final UserSubmitMapper userSubmitMapper = mock(UserSubmitMapper.class);
    private final SupplementContestMapper supplementContestMapper = mock(SupplementContestMapper.class);
    private final ContestProblemSnapshotService snapshotService = mock(ContestProblemSnapshotService.class);
    private final ContestAttemptMapper attemptMapper = mock(ContestAttemptMapper.class);
    private final com.anishan.problem.mapper.ContestRecordsMapper contestRecordsMapper =
            mock(com.anishan.problem.mapper.ContestRecordsMapper.class);
    private final com.anishan.problem.mapper.ContestAnswerRecordsMapper answerRecordsMapper =
            mock(com.anishan.problem.mapper.ContestAnswerRecordsMapper.class);
    private final SubmitLogMapper submitLogMapper = mock(SubmitLogMapper.class);
    private final ProblemEventOutboxMapper outboxMapper = mock(ProblemEventOutboxMapper.class);
    private final SubmitLogService submitLogService = mock(SubmitLogService.class);
    private final ProblemEventOutboxService outboxService = mock(ProblemEventOutboxService.class);

    private ContestSubmissionCoordinator coordinator;
    private Contest contest;

    @BeforeEach
    void setUp() {
        coordinator = new ContestSubmissionCoordinator(contestMapper, userContestMapper, userSubmitMapper,
                supplementContestMapper, snapshotService, attemptMapper, contestRecordsMapper, answerRecordsMapper,
                submitLogMapper, outboxMapper,
                submitLogService, outboxService, CLOCK);
        contest = contest(ContestType.CONTEST, NOW, NOW.plusHours(1));
        when(contestMapper.selectContestForUpdate(CONTEST_ID)).thenReturn(contest);
        when(userContestMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(userSubmitMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(snapshotService.createIfAbsent(CONTEST_ID)).thenReturn(
                Collections.singletonList(snapshot(PROBLEM_ID, ProblemType.OJ.getValue(), "25.00")));
        when(contestMapper.updateNextAttemptSeq(CONTEST_ID, 0L, 1L)).thenReturn(1);
        when(submitLogService.createQueued(any(JudgeInfo.class))).thenReturn(SUBMIT_ID);
        when(attemptMapper.insert(any(ContestAttempt.class))).thenReturn(1);
        com.anishan.problem.domain.entity.ContestRecords projection = new com.anishan.problem.domain.entity.ContestRecords();
        projection.setRecordId(82001L);
        when(contestRecordsMapper.selectForUpdate(CONTEST_ID, USER_ID, PROBLEM_ID)).thenReturn(projection);
        when(outboxService.recordJudgeDispatch(any(JudgeInfo.class))).thenReturn("judge-event-id");
    }

    @Test
    void startBoundaryIsInclusiveAndUsesFrozenScoreAndStableLongSubmitId() throws Exception {
        ContestSubmissionContext accepted = coordinator.acceptOjSubmission(judgeInfo(CONTEST_ID));

        assertEquals(SUBMIT_ID, accepted.getSubmitId());
        assertEquals(1L, accepted.getAttemptSeq());
        assertEquals(new BigDecimal("25.00"), accepted.getMaxScore());
        assertEquals(NOW, accepted.getAcceptedAt());
        verify(contestMapper).updateNextAttemptSeq(CONTEST_ID, 0L, 1L);
        verify(submitLogService).createQueued(argThat(info ->
                new BigDecimal("25.00").equals(info.getListScore()) && info.getContestId().equals(CONTEST_ID)));

        ProblemJudgeResult result = new ProblemJudgeResult();
        result.setSubmitId(accepted.getSubmitId());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode response = objectMapper.readTree(objectMapper.writeValueAsBytes(result));
        assertEquals(Long.toString(SUBMIT_ID), response.get("submitId").asText());
    }

    @Test
    void endBoundaryIsExclusiveAndReturnsHttp409WithoutCreatingLog() {
        contest.setStartTime(NOW.minusMinutes(1));
        contest.setEndTime(NOW);

        ApiStatusException error = assertThrows(ApiStatusException.class,
                () -> coordinator.acceptOjSubmission(judgeInfo(CONTEST_ID)));

        assertEquals(409, error.getStatusCode());
        verify(submitLogService, never()).createQueued(any());
        verify(outboxService, never()).recordJudgeDispatch(any());
    }

    @Test
    void rejectsNonMemberHandedInAndProblemOutsideFrozenRoster() {
        when(userContestMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        ApiStatusException notMember = assertThrows(ApiStatusException.class,
                () -> coordinator.acceptOjSubmission(judgeInfo(CONTEST_ID)));
        assertEquals(403, notMember.getStatusCode());

        when(userContestMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(userSubmitMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        ApiStatusException handedIn = assertThrows(ApiStatusException.class,
                () -> coordinator.acceptOjSubmission(judgeInfo(CONTEST_ID)));
        assertEquals(409, handedIn.getStatusCode());

        when(userSubmitMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(snapshotService.createIfAbsent(CONTEST_ID)).thenReturn(Collections.emptyList());
        ApiStatusException wrongProblem = assertThrows(ApiStatusException.class,
                () -> coordinator.acceptOjSubmission(judgeInfo(CONTEST_ID)));
        assertEquals(409, wrongProblem.getStatusCode());
        verify(submitLogService, never()).createQueued(any());
    }

    @Test
    void homeworkUsesOnlyAValidPerUserSupplementDeadline() {
        contest.setType(ContestType.HOMEWORK);
        contest.setStartTime(NOW.minusHours(2));
        contest.setEndTime(NOW.minusMinutes(10));
        SupplementContest supplement = new SupplementContest();
        supplement.setDeadline(NOW.plusMinutes(10));
        when(supplementContestMapper.selectOne(any())).thenReturn(supplement);

        ContestSubmissionContext accepted = coordinator.acceptOjSubmission(judgeInfo(CONTEST_ID));

        assertEquals(1L, accepted.getAttemptSeq());
        verify(supplementContestMapper).selectOne(any());
    }

    @Test
    void invalidHomeworkExtensionFallsBackToActivityEnd() {
        contest.setType(ContestType.HOMEWORK);
        contest.setStartTime(NOW.minusHours(2));
        contest.setEndTime(NOW.minusMinutes(10));
        SupplementContest invalid = new SupplementContest();
        invalid.setDeadline(NOW.minusMinutes(20));
        when(supplementContestMapper.selectOne(any())).thenReturn(invalid);

        ApiStatusException error = assertThrows(ApiStatusException.class,
                () -> coordinator.acceptOjSubmission(judgeInfo(CONTEST_ID)));

        assertEquals(409, error.getStatusCode());
        verify(submitLogService, never()).createQueued(any());
    }

    @Test
    void homeworkSupplementDeadlineIsExclusiveAtTheExactInstant() {
        contest.setType(ContestType.HOMEWORK);
        contest.setStartTime(NOW.minusHours(2));
        contest.setEndTime(NOW.minusMinutes(10));
        SupplementContest supplement = new SupplementContest();
        supplement.setDeadline(NOW);
        when(supplementContestMapper.selectOne(any())).thenReturn(supplement);

        ApiStatusException error = assertThrows(ApiStatusException.class,
                () -> coordinator.acceptOjSubmission(judgeInfo(CONTEST_ID)));

        assertEquals(409, error.getStatusCode());
        verify(submitLogService, never()).createQueued(any());
    }

    @Test
    void practiceSubmissionPersistsDispatchButDoesNotCreateContestAttempt() {
        ContestSubmissionContext accepted = coordinator.acceptOjSubmission(judgeInfo(null));

        assertNull(accepted.getContestId());
        assertNull(accepted.getAttemptSeq());
        assertNull(accepted.getMaxScore());
        verify(contestMapper, never()).selectContestForUpdate(any());
        verify(snapshotService, never()).createIfAbsent(any());
        verify(attemptMapper, never()).insert(any());
        verify(outboxService).recordJudgeDispatch(argThat(info -> SUBMIT_ID.equals(info.getSubmitId())));
    }

    @Test
    void failedOutboxWritePropagatesFromTransactionalAcceptanceAfterLogAndAttemptWrites() throws Exception {
        when(outboxService.recordJudgeDispatch(any(JudgeInfo.class)))
                .thenThrow(new IllegalStateException("outbox unavailable"));

        assertThrows(IllegalStateException.class,
                () -> coordinator.acceptOjSubmission(judgeInfo(CONTEST_ID)));

        org.mockito.InOrder order = inOrder(submitLogService, attemptMapper, outboxService);
        order.verify(submitLogService).createQueued(any(JudgeInfo.class));
        order.verify(attemptMapper).insert(any(ContestAttempt.class));
        order.verify(outboxService).recordJudgeDispatch(any(JudgeInfo.class));
        Transactional tx = ContestSubmissionCoordinator.class
                .getMethod("acceptOjSubmission", JudgeInfo.class).getAnnotation(Transactional.class);
        assertNotNull(tx);
        assertArrayEquals(new Class<?>[]{Exception.class}, tx.rollbackFor());
    }

    private JudgeInfo judgeInfo(Long contestId) {
        return new JudgeInfo().setSubmitId(null).setUserId(USER_ID).setProblemId(PROBLEM_ID)
                .setContestId(contestId).setLanguageId(1L).setLanguage("Java")
                .setCode("class Main {}");
    }

    private Contest contest(ContestType type, LocalDateTime start, LocalDateTime end) {
        Contest value = new Contest();
        value.setContestId(CONTEST_ID);
        value.setListId(80L);
        value.setType(type);
        value.setStartTime(start);
        value.setEndTime(end);
        value.setNextAttemptSeq(0L);
        return value;
    }

    private ContestProblemSnapshot snapshot(Long problemId, Integer problemType, String maxScore) {
        ContestProblemSnapshot value = new ContestProblemSnapshot();
        value.setContestId(CONTEST_ID);
        value.setProblemId(problemId);
        value.setProblemType(problemType);
        value.setMaxScore(new BigDecimal(maxScore));
        return value;
    }
}
