package com.anishan.problem.controller;

import com.anishan.problem.domain.dto.JudgeRequest;
import com.anishan.problem.service.ContestDraftService;
import com.anishan.problem.service.ContestParticipationService;
import com.anishan.problem.service.ContestService;
import com.anishan.problem.service.RecordsService;
import com.anishan.problem.service.UserContestService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecordControllerDraftTest {

    @Test
    void saveEndpointStoresAnAnswerDraftInsteadOfCreatingAGradedAttempt() {
        RecordsService records = mock(RecordsService.class);
        ContestService contests = mock(ContestService.class);
        ContestParticipationService participation = mock(ContestParticipationService.class);
        ContestDraftService drafts = mock(ContestDraftService.class);
        UserContestService userContests = mock(UserContestService.class);
        RecordController controller = new RecordController(records, contests, participation, drafts, userContests);
        when(participation.canUserSubmit(12L, 7L)).thenReturn(true);
        when(drafts.save(eq(12L), eq(7L), eq(99L), any())).thenReturn(true);
        JudgeRequest request = new JudgeRequest();
        request.setContestId(12L);
        request.setProblemId(99L);
        request.setCode("class Main {}");
        request.setLanguageId(1L);

        assertTrue(controller.save(7L, request).getData());

        verify(drafts).save(eq(12L), eq(7L), eq(99L), any());
        verifyNoInteractions(records, contests, userContests);
    }
}
