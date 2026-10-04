package com.anishan.problem.service;

import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.entity.ContestRecords;
import com.anishan.problem.domain.vo.UserAnswer;
import com.anishan.problem.mapper.ContestAnswerRecordsMapper;
import com.anishan.problem.mapper.ContestRecordsMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Saves only a participant's current answer; it never allocates an attempt or completion award. */
@Service
public class ContestDraftService {

    private final ContestSubmissionCoordinator coordinator;
    private final ContestRecordsMapper contestRecordsMapper;
    private final ContestAnswerRecordsMapper answerRecordsMapper;

    public ContestDraftService(ContestSubmissionCoordinator coordinator,
                               ContestRecordsMapper contestRecordsMapper,
                               ContestAnswerRecordsMapper answerRecordsMapper) {
        this.coordinator = coordinator;
        this.contestRecordsMapper = contestRecordsMapper;
        this.answerRecordsMapper = answerRecordsMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean save(Long contestId, Long userId, Long problemId, UserAnswer answer) {
        if (answer == null) {
            throw new ApiStatusException(400, "作答内容不能为空");
        }
        coordinator.prepareDraft(contestId, userId, problemId);
        contestRecordsMapper.insertDraftIfAbsent(IdWorker.getId(), contestId, userId, problemId);
        ContestRecords projection = contestRecordsMapper.selectForUpdate(contestId, userId, problemId);
        if (projection == null || projection.getRecordId() == null) {
            throw new IllegalStateException("Draft score projection was not initialized");
        }
        answerRecordsMapper.upsertAnswer(projection.getRecordId(), answer);
        return true;
    }
}
