package com.anishan.problem.service;

import com.anishan.api.client.judgeserver.domain.JudgeCaseResult;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.util.RedisJudgeSubmissionLock;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.ContestAttempt;
import com.anishan.problem.domain.entity.ContestRecords;
import com.anishan.problem.domain.entity.JudgeCaseLog;
import com.anishan.problem.domain.entity.Records;
import com.anishan.problem.domain.entity.SubmitLog;
import com.anishan.problem.domain.enumeration.ContestAttemptState;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestRecordsMapper;
import com.anishan.problem.mapper.JudgeCaseLogMapper;
import com.anishan.problem.mapper.SubmitLogMapper;
import com.anishan.problem.domain.vo.UserAnswer;
import com.anishan.problem.domain.dto.JudgeAnswer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JudgeResultApplicationTest {

    private static final long SUBMIT_ID = 501L;
    private static final long CONTEST_ID = 61L;
    private static final long USER_ID = 71L;
    private static final long PROBLEM_ID = 81L;

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void appliesContestAcOnceAndReleasesTheAdmissionLockOnlyAfterCommit() {
        Fixture fixture = new Fixture();
        fixture.prepareContest(JudgeResult.RUNNING, false);
        TransactionSynchronizationManager.initSynchronization();

        fixture.service.applyFinalResult(acCallback());

        verify(fixture.submitLogs).applyFinalResult(eq(SUBMIT_ID), eq(JudgeResult.ACCEPT), any(), any(),
                isNull(), isNull(), isNull(), eq(1), eq(1), eq(new BigDecimal("10.00")), any());
        verify(fixture.cases).insert(any(JudgeCaseLog.class));
        verify(fixture.attempts).applyPendingAttemptResult(eq(SUBMIT_ID), eq(ContestAttemptState.COMPLETED),
                eq(JudgeResult.ACCEPT), eq(new BigDecimal("10.00")), eq(true), any());
        verify(fixture.contestRecords).upsertGradedProjection(anyLong(), eq(CONTEST_ID), eq(USER_ID),
                eq(PROBLEM_ID), eq(true), eq(new BigDecimal("10.00")), eq(8L), eq(801L));
        verify(fixture.completions).completeOnAc(USER_ID, PROBLEM_ID, new BigDecimal("10.00"));
        verify(fixture.lock, never()).release(anyLong(), anyString());

        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        verify(fixture.lock).release(USER_ID, "submission-token");
    }

    @Test
    void duplicateWithDifferentVerdictDoesNotRewriteAppliedResultOrCases() {
        Fixture fixture = new Fixture();
        SubmitLog existing = fixture.log(JudgeResult.ACCEPT, true)
                .setScore(new BigDecimal("10.00")).setTotalCount(1).setPassCount(1)
                .setTime(10L).setMemory(20L);
        fixture.prepareLog(existing);
        JudgeScore duplicate = callback(JudgeResult.WRONG_ANSWER, BigDecimal.ZERO, 0,
                new JudgeCaseResult().setCaseId(901L).setCaseIndex(0).setStatus(JudgeResult.WRONG_ANSWER)
                        .setScore(BigDecimal.ZERO).setTime(9L).setMemory(18L));

        fixture.service.applyFinalResult(duplicate);

        verify(fixture.submitLogs, never()).applyFinalResult(anyLong(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any());
        verify(fixture.cases, never()).insert(any(JudgeCaseLog.class));
        verifyNoInteractions(fixture.attempts, fixture.contestRecords, fixture.completions);
    }

    @Test
    void rejectsForgedCallbackIdentityBeforeWritingAnything() {
        Fixture fixture = new Fixture();
        fixture.prepareContest(JudgeResult.RUNNING, false);
        JudgeScore forged = acCallback().setUserId(USER_ID + 1);

        assertThrows(ApiStatusException.class, () -> fixture.service.applyFinalResult(forged));

        verify(fixture.submitLogs, never()).applyFinalResult(anyLong(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any());
        verifyNoInteractions(fixture.attempts, fixture.contestRecords, fixture.cases, fixture.completions);
    }

    @Test
    void rejectsForgedContestIdentityBeforeWritingAnything() {
        Fixture fixture = new Fixture();
        fixture.prepareContest(JudgeResult.RUNNING, false);
        JudgeScore forged = acCallback().setContestId(CONTEST_ID + 1);

        assertThrows(ApiStatusException.class, () -> fixture.service.applyFinalResult(forged));

        verify(fixture.submitLogs, never()).applyFinalResult(anyLong(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any());
        verifyNoInteractions(fixture.attempts, fixture.contestRecords, fixture.cases, fixture.completions);
    }

    @Test
    void rejectsDuplicateCaseIndicesBeforeClaimingSubmission() {
        Fixture fixture = new Fixture();
        JudgeCaseResult first = new JudgeCaseResult().setCaseId(901L).setCaseIndex(0)
                .setStatus(JudgeResult.ACCEPT).setScore(BigDecimal.ONE).setTime(1L).setMemory(1L);
        JudgeCaseResult second = new JudgeCaseResult().setCaseId(902L).setCaseIndex(0)
                .setStatus(JudgeResult.ACCEPT).setScore(BigDecimal.ONE).setTime(1L).setMemory(1L);
        JudgeScore duplicateIndices = acCallback().setTotalCount(2).setPassCount(2)
                .setCaseResults(Arrays.asList(first, second));

        assertThrows(ApiStatusException.class, () -> fixture.service.applyFinalResult(duplicateIndices));
        verifyNoInteractions(fixture.submitLogs, fixture.contestMapper, fixture.attempts,
                fixture.contestRecords, fixture.cases, fixture.completions);
    }

    @Test
    void infrastructureFailureIsInternalOnlyAndDoesNotOverwriteExistingContestProjection() {
        Fixture fixture = new Fixture();
        fixture.prepareContest(JudgeResult.RUNNING, false);
        JudgeScore infrastructureFailure = callback(JudgeResult.JUDGE_ERROR, BigDecimal.ZERO, 0)
                .setErrorMessage("do not expose")
                .setErrorCode("SANDBOX_UNAVAILABLE")
                .setInternalError("private sandbox diagnostic")
                .setCaseResults(Collections.emptyList());

        fixture.service.applyFinalResult(infrastructureFailure);

        verify(fixture.submitLogs).applyFinalResult(eq(SUBMIT_ID), eq(JudgeResult.JUDGE_ERROR), any(), any(),
                isNull(), eq("private sandbox diagnostic"), eq("SANDBOX_UNAVAILABLE"), eq(0), eq(0),
                eq(BigDecimal.ZERO.setScale(2)), any());
        verify(fixture.attempts).applyPendingAttemptResult(eq(SUBMIT_ID), eq(ContestAttemptState.INFRA_ERROR),
                eq(JudgeResult.JUDGE_ERROR), isNull(), eq(false), any());
        verify(fixture.contestRecords, never()).upsertGradedProjection(anyLong(), anyLong(), anyLong(), anyLong(),
                anyBoolean(), any(), anyLong(), anyLong());
        verifyNoInteractions(fixture.completions);
    }

    @Test
    void terminalStatusCannotBeDowngradedByLateIntermediateCallback() {
        Fixture fixture = new Fixture();
        fixture.prepareLog(fixture.log(JudgeResult.ACCEPT, true));

        assertFalse(fixture.service.updateIntermediateStatus(new JudgeScore()
                .setSubmitId(SUBMIT_ID).setUserId(USER_ID).setResult(JudgeResult.RUNNING)));

        verify(fixture.submitLogs, never()).updateIntermediateStatus(anyLong(), any());
        verifyNoInteractions(fixture.attempts);
    }

    @Test
    void standaloneSubmissionDoesNotRequireAContestAttempt() {
        Fixture fixture = new Fixture();
        SubmitLog standalone = fixture.log(JudgeResult.QUEUE, false).setContestId(null);
        when(fixture.submitLogs.selectContestIdBySubmitId(SUBMIT_ID)).thenReturn(null);
        when(fixture.submitLogs.selectById(SUBMIT_ID)).thenReturn(standalone);
        when(fixture.submitLogs.selectByIdForUpdate(SUBMIT_ID)).thenReturn(standalone);
        when(fixture.submitLogs.applyFinalResult(anyLong(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(1);
        when(fixture.cases.insert(any(JudgeCaseLog.class))).thenReturn(1);

        JudgeScore wrong = callback(JudgeResult.WRONG_ANSWER, BigDecimal.ZERO, 0,
                new JudgeCaseResult().setCaseId(901L).setCaseIndex(0).setStatus(JudgeResult.WRONG_ANSWER)
                        .setScore(BigDecimal.ZERO).setTime(9L).setMemory(18L)).setContestId(null);
        fixture.service.applyFinalResult(wrong);

        verify(fixture.records).addRecord(argThat((Records record) -> record.getContestId() == null
                && record.getUserId().equals(USER_ID) && record.getProblemId().equals(PROBLEM_ID)
                && Boolean.FALSE.equals(record.getStatus())
                && "persisted source".equals(record.getAnswer().getCode())));
        verifyNoInteractions(fixture.attempts, fixture.contestRecords, fixture.completions);
    }

    @Test
    void malformedAcCannotClaimTheSubmission() {
        Fixture fixture = new Fixture();
        JudgeScore noCaseProof = acCallback().setCaseResults(Collections.emptyList());

        assertThrows(ApiStatusException.class, () -> fixture.service.applyFinalResult(noCaseProof));
        verifyNoInteractions(fixture.submitLogs, fixture.contestMapper, fixture.attempts,
                fixture.contestRecords, fixture.cases, fixture.completions);
    }

    private JudgeScore acCallback() {
        return callback(JudgeResult.ACCEPT, new BigDecimal("10.00"), 1,
                new JudgeCaseResult().setCaseId(901L).setCaseIndex(0).setStatus(JudgeResult.ACCEPT)
                        .setScore(new BigDecimal("10.000")).setTime(10L).setMemory(20L));
    }

    private JudgeScore callback(JudgeResult result, BigDecimal score, int passed, JudgeCaseResult item) {
        return new JudgeScore().setSubmitId(SUBMIT_ID).setUserId(USER_ID).setProblemId(PROBLEM_ID)
                .setContestId(CONTEST_ID).setSubmissionLockToken("submission-token")
                .setResult(result).setScore(score).setTotalCount(1).setPassCount(passed)
                .setRuntime(10L).setMemory(20L).setCaseResults(Collections.singletonList(item));
    }

    private JudgeScore callback(JudgeResult result, BigDecimal score, int passed) {
        return new JudgeScore().setSubmitId(SUBMIT_ID).setUserId(USER_ID).setProblemId(PROBLEM_ID)
                .setContestId(CONTEST_ID).setSubmissionLockToken("submission-token")
                .setResult(result).setScore(score).setTotalCount(0).setPassCount(passed)
                .setRuntime(10L).setMemory(20L).setCaseResults(Collections.emptyList());
    }

    private final class Fixture {
        final ContestMapper contestMapper = mock(ContestMapper.class);
        final SubmitLogMapper submitLogs = mock(SubmitLogMapper.class);
        final ContestAttemptMapper attempts = mock(ContestAttemptMapper.class);
        final ContestRecordsMapper contestRecords = mock(ContestRecordsMapper.class);
        final JudgeCaseLogMapper cases = mock(JudgeCaseLogMapper.class);
        final RecordsService records = mock(RecordsService.class);
        final ProblemCompletionAwardService completions = mock(ProblemCompletionAwardService.class);
        final RedisJudgeSubmissionLock lock = mock(RedisJudgeSubmissionLock.class);
        final JudgeResultApplicationService service = new JudgeResultApplicationService(contestMapper, submitLogs,
                attempts, contestRecords, cases, records, completions, lock,
                Clock.fixed(Instant.parse("2026-10-04T00:00:00Z"), ZoneOffset.UTC));

        SubmitLog log(JudgeResult status, boolean applied) {
            return new SubmitLog().setSubmitId(SUBMIT_ID).setUserId(USER_ID).setProblemId(PROBLEM_ID)
                    .setContestId(CONTEST_ID).setCode("persisted source").setStatus(status)
                    .setResultApplied(applied);
        }

        void prepareContest(JudgeResult status, boolean applied) {
            prepareLog(log(status, applied));
            when(contestMapper.selectContestForUpdate(CONTEST_ID)).thenReturn(new Contest());
            ContestAttempt attempt = new ContestAttempt();
            attempt.setAttemptId(801L);
            attempt.setSubmitId(SUBMIT_ID);
            attempt.setContestId(CONTEST_ID);
            attempt.setUserId(USER_ID);
            attempt.setProblemId(PROBLEM_ID);
            attempt.setAttemptSeq(8L);
            attempt.setState(ContestAttemptState.PENDING);
            when(attempts.selectBySubmitIdForUpdate(SUBMIT_ID)).thenReturn(attempt);
            when(contestRecords.selectForUpdate(CONTEST_ID, USER_ID, PROBLEM_ID))
                    .thenReturn(new ContestRecords().setRecordId(701L));
            when(submitLogs.applyFinalResult(anyLong(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any())).thenReturn(1);
            when(cases.insert(any(JudgeCaseLog.class))).thenReturn(1);
            when(attempts.applyPendingAttemptResult(anyLong(), any(), any(), any(), any(), any())).thenReturn(1);
        }

        void prepareLog(SubmitLog row) {
            when(submitLogs.selectContestIdBySubmitId(SUBMIT_ID)).thenReturn(row.getContestId());
            when(submitLogs.selectByIdForUpdate(SUBMIT_ID)).thenReturn(row);
            if (row.getContestId() != null) {
                when(contestMapper.selectContestForUpdate(row.getContestId())).thenReturn(new Contest());
            }
        }
    }
}
