package com.anishan.problem.service;

import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.util.RedisJudgeSubmissionLock;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.problem.domain.entity.SubmitLog;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestRecordsMapper;
import com.anishan.problem.mapper.JudgeCaseLogMapper;
import com.anishan.problem.mapper.SubmitLogMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JudgeStatusTransitionTest {

    private static final long SUBMIT_ID = 1701L;
    private static final long USER_ID = 1702L;

    @Test
    void onlyAllowsForwardIntermediateTransitionsAndNeverDowngradesTerminalStatus() {
        ContestMapper contests = mock(ContestMapper.class);
        SubmitLogMapper submissions = mock(SubmitLogMapper.class);
        ContestAttemptMapper attempts = mock(ContestAttemptMapper.class);
        ContestRecordsMapper projections = mock(ContestRecordsMapper.class);
        JudgeCaseLogMapper cases = mock(JudgeCaseLogMapper.class);
        RecordsService records = mock(RecordsService.class);
        ProblemCompletionAwardService awards = mock(ProblemCompletionAwardService.class);
        RedisJudgeSubmissionLock lock = mock(RedisJudgeSubmissionLock.class);
        JudgeResultApplicationService service = new JudgeResultApplicationService(contests, submissions,
                attempts, projections, cases, records, awards, lock,
                Clock.fixed(Instant.parse("2026-10-04T00:00:00Z"), ZoneOffset.UTC));

        SubmitLog queued = submission(JudgeResult.QUEUE);
        SubmitLog compiling = submission(JudgeResult.COMPILING);
        SubmitLog running = submission(JudgeResult.RUNNING);
        SubmitLog terminal = submission(JudgeResult.ACCEPT).setResultApplied(true);
        when(submissions.selectContestIdBySubmitId(SUBMIT_ID)).thenReturn(null);
        when(submissions.selectById(SUBMIT_ID)).thenReturn(queued);
        when(submissions.selectByIdForUpdate(SUBMIT_ID)).thenReturn(queued, compiling, running, terminal);
        when(submissions.updateIntermediateStatus(SUBMIT_ID, JudgeResult.COMPILING)).thenReturn(1);
        when(submissions.updateIntermediateStatus(SUBMIT_ID, JudgeResult.RUNNING)).thenReturn(1);

        assertTrue(service.updateIntermediateStatus(callback(JudgeResult.COMPILING)));
        assertTrue(service.updateIntermediateStatus(callback(JudgeResult.RUNNING)));
        assertFalse(service.updateIntermediateStatus(callback(JudgeResult.COMPILING)));
        assertFalse(service.updateIntermediateStatus(callback(JudgeResult.RUNNING)));

        verify(submissions).updateIntermediateStatus(SUBMIT_ID, JudgeResult.COMPILING);
        verify(submissions, times(1)).updateIntermediateStatus(eq(SUBMIT_ID), eq(JudgeResult.RUNNING));
    }

    private SubmitLog submission(JudgeResult status) {
        return new SubmitLog().setSubmitId(SUBMIT_ID).setUserId(USER_ID).setContestId(null)
                .setStatus(status).setResultApplied(status == JudgeResult.ACCEPT);
    }

    private JudgeScore callback(JudgeResult result) {
        return new JudgeScore().setSubmitId(SUBMIT_ID).setUserId(USER_ID).setResult(result);
    }
}
