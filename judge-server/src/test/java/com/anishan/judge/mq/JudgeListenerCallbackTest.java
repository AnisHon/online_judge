package com.anishan.judge.mq;

import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.problem.client.ProblemInternalClient;
import com.anishan.api.client.problem.domain.vo.JudgeSubmissionStatusVo;
import com.anishan.commons.domain.R;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.judge.judge.JudgeRun;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JudgeListenerCallbackTest {

    @Test
    void skipsAQueueDeliveryWhoseResultWasAlreadyApplied() {
        JudgeRun runner = mock(JudgeRun.class);
        ProblemInternalClient problem = mock(ProblemInternalClient.class);
        JudgeListener listener = new JudgeListener(runner, problem);
        JudgeInfo message = message();
        when(problem.judgeSubmissionStatus(31L)).thenReturn(R.success(status(true)));

        listener.judge(message);

        verify(runner, never()).judgeAll(any());
        verify(problem, never()).judgeResult(any());
    }

    @Test
    void judgesAndReturnsTheOriginalSubmissionIdentityForUnappliedDelivery() {
        JudgeRun runner = mock(JudgeRun.class);
        ProblemInternalClient problem = mock(ProblemInternalClient.class);
        JudgeListener listener = new JudgeListener(runner, problem);
        JudgeInfo message = message();
        when(problem.judgeSubmissionStatus(31L)).thenReturn(R.success(status(false)));
        when(runner.judgeAll(message)).thenReturn(new JudgeScore().setResult(JudgeResult.ACCEPT)
                .setScore(new BigDecimal("10.00")));

        listener.judge(message);

        verify(problem).judgeStatus(argThat(score -> score.getSubmitId().equals(31L)
                && score.getUserId().equals(41L) && score.getResult() == JudgeResult.COMPILING));
        verify(problem).judgeResult(argThat(score -> score.getSubmitId().equals(31L)
                && score.getUserId().equals(41L) && score.getProblemId().equals(51L)
                && score.getContestId().equals(61L) && score.getSubmissionLockToken().equals("lock-token")
                && score.getCode().equals("class Main {}") && score.getResult() == JudgeResult.ACCEPT));
    }

    @Test
    void retriesRatherThanJudgingWhenInternalStatusCannotBeRead() {
        JudgeRun runner = mock(JudgeRun.class);
        ProblemInternalClient problem = mock(ProblemInternalClient.class);
        JudgeListener listener = new JudgeListener(runner, problem);
        when(problem.judgeSubmissionStatus(31L)).thenReturn(R.error(503, "unavailable"));

        assertThrows(IllegalStateException.class, () -> listener.judge(message()));
        verify(runner, never()).judgeAll(any());
        verify(problem, never()).judgeResult(any());
    }

    private JudgeInfo message() {
        return new JudgeInfo().setSubmitId(31L).setUserId(41L).setProblemId(51L)
                .setContestId(61L).setLanguageId(71L).setLanguage("java")
                .setCode("class Main {}").setSubmissionLockToken("lock-token");
    }

    private JudgeSubmissionStatusVo status(boolean applied) {
        JudgeSubmissionStatusVo status = new JudgeSubmissionStatusVo();
        status.setResultApplied(applied);
        status.setStatus(applied ? JudgeResult.ACCEPT : JudgeResult.RUNNING);
        return status;
    }
}
