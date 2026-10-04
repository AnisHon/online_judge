package com.anishan.problem.service;

import com.anishan.problem.domain.ContestSubmissionContext;
import com.anishan.problem.domain.vo.ProblemJudgeResult;
import com.anishan.problem.domain.vo.UserAnswer;
import com.anishan.problem.mapper.ContestAnswerRecordsMapper;
import com.anishan.problem.mapper.ContestRecordsMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies one synchronous non-OJ activity answer as a completed, sequenced attempt. */
@Service
public class ContestAnswerSubmissionService {

    private final ContestSubmissionCoordinator coordinator;
    private final ContestRecordsMapper contestRecordsMapper;
    private final ContestAnswerRecordsMapper answerRecordsMapper;
    private final ProblemCompletionAwardService completionAwardService;

    public ContestAnswerSubmissionService(ContestSubmissionCoordinator coordinator,
                                          ContestRecordsMapper contestRecordsMapper,
                                          ContestAnswerRecordsMapper answerRecordsMapper,
                                          ProblemCompletionAwardService completionAwardService) {
        this.coordinator = coordinator;
        this.contestRecordsMapper = contestRecordsMapper;
        this.answerRecordsMapper = answerRecordsMapper;
        this.completionAwardService = completionAwardService;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProblemJudgeResult submit(Long contestId, Long userId, Long problemId,
                                     ProblemJudgeResult result, UserAnswer answer) {
        if (result == null) {
            throw new IllegalArgumentException("A judged answer is required");
        }
        ContestSubmissionContext accepted = coordinator.acceptNonOjAnswer(contestId, userId, problemId,
                result.getTotalScore(), result.getFullMark(), result.isCorrect());
        contestRecordsMapper.upsertGradedProjection(IdWorker.getId(), contestId, userId, problemId,
                result.isCorrect(), accepted.getScore(), accepted.getAttemptSeq(), accepted.getAttemptId());
        com.anishan.problem.domain.entity.ContestRecords projection = contestRecordsMapper.selectForUpdate(
                contestId, userId, problemId);
        if (projection == null || projection.getRecordId() == null) {
            throw new IllegalStateException("Graded score projection was not persisted");
        }
        answerRecordsMapper.upsertAnswer(projection.getRecordId(), answer);
        if (result.isCorrect()) {
            completionAwardService.completeOnAc(userId, problemId, accepted.getScore());
        }

        // Keep the existing response shape while hiding contest feedback and standard answers.
        result.setTotalScore(accepted.getScore());
        result.setFullMark(accepted.getMaxScore());
        result.setAnswers(null);
        result.setCorrect(false);
        return result;
    }
}
