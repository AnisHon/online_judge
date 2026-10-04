package com.anishan.problem.service.impl;

import com.anishan.api.file.FileOperation;
import com.anishan.commons.exception.BusinessException;
import com.anishan.problem.mapper.ContestRecordsMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestMutationReferenceMapper;
import com.anishan.problem.mapper.ChoiceFillAnswersMapper;
import com.anishan.problem.mapper.OjProblemCaseMapper;
import com.anishan.problem.mapper.ProblemMapper;
import com.anishan.problem.mapper.ProblemProblemListMapper;
import com.anishan.problem.mapper.RecordsMapper;
import com.anishan.problem.mapper.SubmitLogMapper;
import com.anishan.problem.service.ChoiceFillAnswersService;
import com.anishan.problem.service.ContestMutationGuard;
import com.anishan.problem.service.OjProblemCaseService;
import com.anishan.problem.service.OjProblemService;
import com.anishan.problem.service.TagService;
import com.anishan.problem.util.ProblemUploadUtil;
import org.junit.jupiter.api.Test;
import com.anishan.problem.domain.entity.Problem;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.time.Clock;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProblemDeletionGuardTest {
    @Test
    void submissionHistoryStillPreventsProblemDeletion() {
        SubmitLogMapper submitLogs = mock(SubmitLogMapper.class);
        RecordsMapper records = mock(RecordsMapper.class);
        ContestRecordsMapper contestRecords = mock(ContestRecordsMapper.class);
        when(submitLogs.selectCount(any())).thenReturn(1L);
        ProblemServiceImpl service = service(submitLogs, records, contestRecords);

        assertThrows(BusinessException.class, () -> service.removeProblems(Collections.singletonList(7L)));

        verify(records, never()).selectCount(any());
        verify(contestRecords, never()).selectCount(any());
        verify(service, never()).removeByIds(any());
    }

    @Test
    void answerHistoryStillPreventsProblemDeletion() {
        SubmitLogMapper submitLogs = mock(SubmitLogMapper.class);
        RecordsMapper records = mock(RecordsMapper.class);
        ContestRecordsMapper contestRecords = mock(ContestRecordsMapper.class);
        when(submitLogs.selectCount(any())).thenReturn(0L);
        when(records.selectCount(any())).thenReturn(1L);
        ProblemServiceImpl service = service(submitLogs, records, contestRecords);

        assertThrows(BusinessException.class, () -> service.removeProblems(Collections.singletonList(7L)));

        verify(contestRecords, never()).selectCount(any());
        verify(service, never()).removeByIds(any());
    }

    @Test
    void contestAnswerHistoryStillPreventsProblemDeletion() {
        SubmitLogMapper submitLogs = mock(SubmitLogMapper.class);
        RecordsMapper records = mock(RecordsMapper.class);
        ContestRecordsMapper contestRecords = mock(ContestRecordsMapper.class);
        when(submitLogs.selectCount(any())).thenReturn(0L);
        when(records.selectCount(any())).thenReturn(0L);
        when(contestRecords.selectCount(any())).thenReturn(1L);
        ProblemServiceImpl service = service(submitLogs, records, contestRecords);

        assertThrows(BusinessException.class, () -> service.removeProblems(Collections.singletonList(7L)));

        verify(service, never()).removeByIds(any());
    }

    private ProblemServiceImpl service(SubmitLogMapper submitLogs, RecordsMapper records,
                                       ContestRecordsMapper contestRecords) {
        ProblemMapper problems = mock(ProblemMapper.class);
        when(problems.selectByProblemIdsForUpdate(anyList())).thenAnswer(invocation -> {
            Problem problem = new Problem();
            problem.setProblemId(7L);
            problem.setDelFlag(0);
            return Collections.singletonList(problem);
        });
        OjProblemCaseMapper cases = mock(OjProblemCaseMapper.class);
        when(cases.selectList(org.mockito.ArgumentMatchers.<Wrapper<com.anishan.api.domain.entity.OjProblemCase>>any()))
                .thenReturn(Collections.emptyList());
        ContestMutationGuard guard = new ContestMutationGuard(
                mock(ContestMutationReferenceMapper.class), mock(ContestMapper.class), problems, cases,
                Clock.systemUTC());
        return org.mockito.Mockito.spy(new ProblemServiceImpl(
                mock(OjProblemService.class), problems, mock(ChoiceFillAnswersService.class),
                mock(TagService.class), mock(OjProblemCaseService.class), mock(ProblemProblemListMapper.class),
                submitLogs, records, contestRecords, mock(ProblemUploadUtil.class), mock(FileOperation.class),
                guard, mock(ChoiceFillAnswersMapper.class)));
    }
}
