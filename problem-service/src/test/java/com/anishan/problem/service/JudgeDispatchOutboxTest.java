package com.anishan.problem.service;

import com.anishan.api.util.RedisJudgeSubmissionLock;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.ContestAttempt;
import com.anishan.problem.domain.entity.ProblemEventOutbox;
import com.anishan.problem.domain.entity.SubmitLog;
import com.anishan.problem.domain.enumeration.ContestAttemptState;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ProblemEventOutboxMapper;
import com.anishan.problem.mapper.SubmitLogMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JudgeDispatchOutboxTest {

    private static final Long SUBMIT_ID = 2098931802871431170L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void tokenLockIsReleasedOnAcceptanceFailureButRetainedAfterDurableAcceptance() {
        RedisJudgeSubmissionLock lock = mock(RedisJudgeSubmissionLock.class);
        ContestSubmissionCoordinator coordinator = mock(ContestSubmissionCoordinator.class);
        JudgeSubmissionService service = new JudgeSubmissionService(lock, coordinator);
        when(lock.tryAcquire(10L)).thenReturn("lock-token");
        when(coordinator.acceptOjSubmission(any())).thenThrow(new ApiStatusException(409, "closed"));

        assertThrows(ApiStatusException.class, () -> service.submit(judgeInfo(10L)));
        verify(lock).release(10L, "lock-token");

        reset(lock, coordinator);
        when(lock.tryAcquire(10L)).thenReturn("second-token");
        when(coordinator.acceptOjSubmission(any())).thenReturn(new com.anishan.problem.domain.ContestSubmissionContext()
                .setSubmitId(SUBMIT_ID));
        assertEquals(SUBMIT_ID, service.submit(judgeInfo(10L)).getSubmitId());
        verify(lock, never()).release(any(), any());
    }

    @Test
    void failedUnappliedDispatchRequeuesSameEventAndDuplicateRetryCannotResetItAgain() {
        Fixture f = new Fixture();
        SubmitLog log = pendingLog(null);
        ProblemEventOutbox event = failedEvent();
        when(f.submitLogMapper.selectById(SUBMIT_ID)).thenReturn(log);
        when(f.submitLogMapper.selectByIdForUpdate(SUBMIT_ID)).thenReturn(log);
        when(f.outboxMapper.selectByDedupeKeyForUpdate("judge-dispatch:" + SUBMIT_ID)).thenReturn(event);
        when(f.outboxMapper.retryFailedJudgeDispatch(event.getEventId(), event.getDedupeKey(), NOW)).thenReturn(1);

        assertTrue(f.coordinator.retryFailedDispatch(SUBMIT_ID));
        verify(f.outboxMapper).retryFailedJudgeDispatch(event.getEventId(), event.getDedupeKey(), NOW);

        event.setStatus("PENDING");
        ApiStatusException duplicate = assertThrows(ApiStatusException.class,
                () -> f.coordinator.retryFailedDispatch(SUBMIT_ID));
        assertEquals(409, duplicate.getStatusCode());
        verify(f.outboxMapper, times(1)).retryFailedJudgeDispatch(anyString(), anyString(), any());
    }

    @Test
    void terminalOrAlreadyAppliedSubmissionCannotBeRedispatched() {
        Fixture f = new Fixture();
        SubmitLog completed = pendingLog(null).setStatus(JudgeResult.ACCEPT);
        when(f.submitLogMapper.selectById(SUBMIT_ID)).thenReturn(completed);
        when(f.submitLogMapper.selectByIdForUpdate(SUBMIT_ID)).thenReturn(completed);

        assertEquals(409, assertThrows(ApiStatusException.class,
                () -> f.coordinator.retryFailedDispatch(SUBMIT_ID)).getStatusCode());
        verify(f.outboxMapper, never()).selectByDedupeKeyForUpdate(anyString());

        SubmitLog appliedQueue = pendingLog(null).setResultApplied(true);
        when(f.submitLogMapper.selectById(SUBMIT_ID)).thenReturn(appliedQueue);
        when(f.submitLogMapper.selectByIdForUpdate(SUBMIT_ID)).thenReturn(appliedQueue);
        assertEquals(409, assertThrows(ApiStatusException.class,
                () -> f.coordinator.retryFailedDispatch(SUBMIT_ID)).getStatusCode());
        verify(f.outboxMapper, never()).retryFailedJudgeDispatch(anyString(), anyString(), any());
    }

    @Test
    void contestRetryLocksContestLogAttemptThenOutboxAndRequiresPendingAttempt() {
        Fixture f = new Fixture();
        SubmitLog log = pendingLog(77L);
        Contest contest = new Contest();
        contest.setContestId(77L);
        ContestAttempt attempt = pendingAttempt();
        ProblemEventOutbox event = failedEvent();
        when(f.submitLogMapper.selectById(SUBMIT_ID)).thenReturn(log);
        when(f.contestMapper.selectContestForUpdate(77L)).thenReturn(contest);
        when(f.submitLogMapper.selectByIdForUpdate(SUBMIT_ID)).thenReturn(log);
        when(f.attemptMapper.selectBySubmitIdForUpdate(SUBMIT_ID)).thenReturn(attempt);
        when(f.outboxMapper.selectByDedupeKeyForUpdate("judge-dispatch:" + SUBMIT_ID)).thenReturn(event);
        when(f.outboxMapper.retryFailedJudgeDispatch(event.getEventId(), event.getDedupeKey(), NOW)).thenReturn(1);

        assertTrue(f.coordinator.retryFailedDispatch(SUBMIT_ID));

        org.mockito.InOrder order = inOrder(f.contestMapper, f.submitLogMapper,
                f.attemptMapper, f.outboxMapper);
        order.verify(f.contestMapper).selectContestForUpdate(77L);
        order.verify(f.submitLogMapper).selectByIdForUpdate(SUBMIT_ID);
        order.verify(f.attemptMapper).selectBySubmitIdForUpdate(SUBMIT_ID);
        order.verify(f.outboxMapper).selectByDedupeKeyForUpdate("judge-dispatch:" + SUBMIT_ID);

        ContestAttempt completedAttempt = pendingAttempt();
        completedAttempt.setState(ContestAttemptState.COMPLETED);
        when(f.attemptMapper.selectBySubmitIdForUpdate(SUBMIT_ID)).thenReturn(completedAttempt);
        assertEquals(409, assertThrows(ApiStatusException.class,
                () -> f.coordinator.retryFailedDispatch(SUBMIT_ID)).getStatusCode());
    }

    @Test
    void mapperSqlFencesSequenceUpdateAndOnlyReplaysFailedJudgeRows() throws Exception {
        Configuration configuration = new Configuration();
        parse(configuration, "/mapper/ContestMapper.xml");
        parse(configuration, "/mapper/ContestAttemptMapper.xml");
        parse(configuration, "/mapper/ProblemEventOutboxMapper.xml");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("contestId", 77L);
        parameters.put("expectedSeq", 4L);
        parameters.put("nextSeq", 5L);
        parameters.put("eventId", UUID.randomUUID().toString());
        parameters.put("dedupeKey", "judge-dispatch:" + SUBMIT_ID);
        parameters.put("now", NOW);

        String sequenceSql = normalized(configuration.getMappedStatement(
                "com.anishan.problem.mapper.ContestMapper.updateNextAttemptSeq")
                .getBoundSql(parameters).getSql());
        assertTrue(sequenceSql.contains("coalesce(next_attempt_seq, 0) = ?"));
        assertTrue(sequenceSql.contains("contest_id = ?"));

        String retrySql = normalized(configuration.getMappedStatement(
                "com.anishan.problem.mapper.ProblemEventOutboxMapper.retryFailedJudgeDispatch")
                .getBoundSql(parameters).getSql());
        assertTrue(retrySql.contains("status = 'pending'"));
        assertTrue(retrySql.contains("attempts = 0"));
        assertTrue(retrySql.contains("event_type = 'judge_dispatch'"));
        assertTrue(retrySql.contains("status = 'failed'"));
        assertTrue(retrySql.contains("dedupe_key = ?"));

        Transactional tx = ContestSubmissionCoordinator.class
                .getMethod("retryFailedDispatch", Long.class).getAnnotation(Transactional.class);
        assertNotNull(tx);
        assertArrayEquals(new Class<?>[]{Exception.class}, tx.rollbackFor());
    }

    private static void parse(Configuration configuration, String resource) throws Exception {
        try (InputStream input = JudgeDispatchOutboxTest.class.getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    private static String normalized(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static com.anishan.api.client.judgeserver.domain.JudgeInfo judgeInfo(Long userId) {
        return new com.anishan.api.client.judgeserver.domain.JudgeInfo().setUserId(userId)
                .setProblemId(1L).setLanguageId(1L).setLanguage("Java").setCode("class Main {}");
    }

    private static SubmitLog pendingLog(Long contestId) {
        return new SubmitLog().setSubmitId(SUBMIT_ID).setUserId(10L).setProblemId(11L)
                .setContestId(contestId).setStatus(JudgeResult.QUEUE).setResultApplied(false);
    }

    private static ContestAttempt pendingAttempt() {
        ContestAttempt attempt = new ContestAttempt();
        attempt.setSubmitId(SUBMIT_ID);
        attempt.setKind("OJ");
        attempt.setState(ContestAttemptState.PENDING);
        return attempt;
    }

    private static ProblemEventOutbox failedEvent() {
        return new ProblemEventOutbox().setEventId(UUID.randomUUID().toString())
                .setDedupeKey("judge-dispatch:" + SUBMIT_ID)
                .setEventType(ProblemEventOutboxService.JUDGE_DISPATCH)
                .setStatus("FAILED").setAttempts(20);
    }

    private static final class Fixture {
        private final ContestMapper contestMapper = mock(ContestMapper.class);
        private final ContestAttemptMapper attemptMapper = mock(ContestAttemptMapper.class);
        private final SubmitLogMapper submitLogMapper = mock(SubmitLogMapper.class);
        private final ProblemEventOutboxMapper outboxMapper = mock(ProblemEventOutboxMapper.class);
        private final ContestSubmissionCoordinator coordinator = new ContestSubmissionCoordinator(
                contestMapper, mock(com.anishan.problem.mapper.UserContestMapper.class),
                mock(com.anishan.problem.mapper.UserSubmitMapper.class),
                mock(com.anishan.problem.mapper.SupplementContestMapper.class),
                mock(ContestProblemSnapshotService.class), attemptMapper,
                mock(com.anishan.problem.mapper.ContestRecordsMapper.class),
                mock(com.anishan.problem.mapper.ContestAnswerRecordsMapper.class),
                submitLogMapper, outboxMapper,
                mock(SubmitLogService.class), mock(ProblemEventOutboxService.class), CLOCK);
    }
}
