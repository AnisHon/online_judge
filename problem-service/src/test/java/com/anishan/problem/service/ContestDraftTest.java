package com.anishan.problem.service;

import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.ContestSubmissionContext;
import com.anishan.problem.domain.entity.ContestRecords;
import com.anishan.problem.domain.vo.UserAnswer;
import com.anishan.problem.mapper.ContestAnswerRecordsMapper;
import com.anishan.problem.mapper.ContestRecordsMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContestDraftTest {

    @Test
    void draftSavesOnlyAnswerAndLeavesAnExistingGradeProjectionUntouched() {
        ContestSubmissionCoordinator coordinator = mock(ContestSubmissionCoordinator.class);
        ContestRecordsMapper records = mock(ContestRecordsMapper.class);
        ContestAnswerRecordsMapper answers = mock(ContestAnswerRecordsMapper.class);
        ContestDraftService service = new ContestDraftService(coordinator, records, answers);
        when(coordinator.prepareDraft(12L, 7L, 99L))
                .thenReturn(new ContestSubmissionContext().setContestId(12L).setUserId(7L).setProblemId(99L));
        ContestRecords graded = new ContestRecords();
        graded.setRecordId(777L);
        graded.setScore(java.math.BigDecimal.TEN);
        graded.setStatus(true);
        graded.setAppliedAttemptSeq(6L);
        when(records.selectForUpdate(12L, 7L, 99L)).thenReturn(graded);
        UserAnswer answer = new UserAnswer();

        assertTrue(service.save(12L, 7L, 99L, answer));

        verify(records).insertDraftIfAbsent(anyLong(), eq(12L), eq(7L), eq(99L));
        verify(records, never()).upsertGradedProjection(anyLong(), anyLong(), anyLong(), anyLong(),
                any(), any(), anyLong(), anyLong());
        verify(answers).upsertAnswer(777L, answer);
        assertEquals(java.math.BigDecimal.TEN, graded.getScore());
        assertEquals(true, graded.getStatus());
        assertEquals(6L, graded.getAppliedAttemptSeq());
    }

    @Test
    void nullDraftIsRejectedBeforePersistence() {
        ContestDraftService service = new ContestDraftService(mock(ContestSubmissionCoordinator.class),
                mock(ContestRecordsMapper.class), mock(ContestAnswerRecordsMapper.class));
        assertThrows(ApiStatusException.class, () -> service.save(12L, 7L, 99L, null));
    }
}
