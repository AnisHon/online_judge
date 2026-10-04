package com.anishan.problem.service;

import com.anishan.api.util.WeightedScoreCalculator;
import com.anishan.problem.domain.ContestSubmissionContext;
import com.anishan.problem.domain.entity.ContestRecords;
import com.anishan.problem.domain.dto.JudgeAnswer;
import com.anishan.problem.domain.vo.ProblemJudgeResult;
import com.anishan.problem.domain.vo.UserAnswer;
import com.anishan.problem.mapper.ContestAnswerRecordsMapper;
import com.anishan.problem.mapper.ContestRecordsMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContestAnswerScoringTest {

    @Test
    void nonOjContestPersistsAndReturnsTheAcceptedNormalizedScore() {
        ContestSubmissionCoordinator coordinator = mock(ContestSubmissionCoordinator.class);
        ContestRecordsMapper records = mock(ContestRecordsMapper.class);
        ContestAnswerRecordsMapper answers = mock(ContestAnswerRecordsMapper.class);
        ProblemCompletionAwardService awards = mock(ProblemCompletionAwardService.class);
        ContestAnswerSubmissionService service = new ContestAnswerSubmissionService(
                coordinator, records, answers, awards);

        when(coordinator.acceptNonOjAnswer(eq(12L), eq(7L), eq(99L), any(), any(), eq(true)))
                .thenReturn(new ContestSubmissionContext().setAttemptId(900L).setAttemptSeq(3L)
                        .setMaxScore(new BigDecimal("10.000")).setScore(new BigDecimal("3.33")));
        ContestRecords projection = new ContestRecords();
        projection.setRecordId(501L);
        when(records.selectForUpdate(12L, 7L, 99L)).thenReturn(projection);

        ProblemJudgeResult result = new ProblemJudgeResult();
        result.setCorrect(true);
        result.setTotalScore(BigDecimal.ONE);
        result.setFullMark(new BigDecimal("3"));
        result.setAnswers(Collections.singletonList(new JudgeAnswer(1, "secret")));
        ProblemJudgeResult returned = service.submit(12L, 7L, 99L, result,
                new UserAnswer(Collections.singletonList(new JudgeAnswer(1, "answer")), null, null));

        verify(coordinator).acceptNonOjAnswer(12L, 7L, 99L, BigDecimal.ONE,
                new BigDecimal("3"), true);
        verify(records).upsertGradedProjection(anyLong(), eq(12L), eq(7L), eq(99L),
                eq(true), eq(new BigDecimal("3.33")), eq(3L), eq(900L));
        verify(answers).upsertAnswer(eq(501L), any(UserAnswer.class));
        verify(awards).completeOnAc(7L, 99L, new BigDecimal("3.33"));
        assertEquals(new BigDecimal("3.33"), returned.getTotalScore());
        assertEquals(new BigDecimal("10.000"), returned.getFullMark());
        assertFalse(returned.isCorrect());
        assertNull(returned.getAnswers());
    }

    @Test
    void incorrectPartialAnswerIsScoredButDoesNotAwardCompletion() {
        ContestSubmissionCoordinator coordinator = mock(ContestSubmissionCoordinator.class);
        ContestRecordsMapper records = mock(ContestRecordsMapper.class);
        ContestAnswerRecordsMapper answers = mock(ContestAnswerRecordsMapper.class);
        ProblemCompletionAwardService awards = mock(ProblemCompletionAwardService.class);
        ContestAnswerSubmissionService service = new ContestAnswerSubmissionService(
                coordinator, records, answers, awards);
        when(coordinator.acceptNonOjAnswer(anyLong(), anyLong(), anyLong(), any(), any(), eq(false)))
                .thenReturn(new ContestSubmissionContext().setAttemptId(901L).setAttemptSeq(4L)
                        .setMaxScore(new BigDecimal("10.00")).setScore(new BigDecimal("2.00")));
        ContestRecords projection = new ContestRecords();
        projection.setRecordId(502L);
        when(records.selectForUpdate(anyLong(), anyLong(), anyLong())).thenReturn(projection);

        ProblemJudgeResult raw = new ProblemJudgeResult();
        raw.setCorrect(false);
        raw.setTotalScore(new BigDecimal("1"));
        raw.setFullMark(new BigDecimal("5"));
        service.submit(12L, 7L, 99L, raw, new UserAnswer());

        verify(awards, never()).completeOnAc(anyLong(), anyLong(), any());
        verify(records).upsertGradedProjection(anyLong(), anyLong(), anyLong(), anyLong(),
                eq(false), eq(new BigDecimal("2.00")), anyLong(), anyLong());
    }

    @Test
    void sharedNormalizerIsUsedForContestAnswerScoring() {
        assertEquals(new BigDecimal("3.33"), WeightedScoreCalculator.normalize(
                new BigDecimal("1"), new BigDecimal("3"), new BigDecimal("10")));
    }
}
