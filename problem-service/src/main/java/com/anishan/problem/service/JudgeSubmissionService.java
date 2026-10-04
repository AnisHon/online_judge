package com.anishan.problem.service;

import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.util.RedisJudgeSubmissionLock;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.ContestSubmissionContext;
import org.springframework.stereotype.Service;

/** Acquires the existing per-user admission token outside the database transaction. */
@Service
public class JudgeSubmissionService {

    private final RedisJudgeSubmissionLock submissionLock;
    private final ContestSubmissionCoordinator coordinator;

    public JudgeSubmissionService(RedisJudgeSubmissionLock submissionLock,
                                  ContestSubmissionCoordinator coordinator) {
        this.submissionLock = submissionLock;
        this.coordinator = coordinator;
    }

    public ContestSubmissionContext submit(JudgeInfo judgeInfo) {
        if (judgeInfo == null || judgeInfo.getUserId() == null || judgeInfo.getUserId() <= 0) {
            throw new ApiStatusException(400, "判题参数无效");
        }
        String token = submissionLock.tryAcquire(judgeInfo == null ? null : judgeInfo.getUserId());
        if (token == null) {
            throw new ApiStatusException(409, "上一条提交仍在判题，请等待结果后再提交");
        }
        boolean accepted = false;
        try {
            judgeInfo.setSubmissionLockToken(token);
            ContestSubmissionContext context = coordinator.acceptOjSubmission(judgeInfo);
            accepted = true;
            return context;
        } finally {
            if (!accepted) {
                // Token comparison in Redis ensures a failed older request cannot release a newer lock.
                submissionLock.release(judgeInfo.getUserId(), token);
            }
        }
    }

    public boolean retryFailedDispatch(Long submitId) {
        return coordinator.retryFailedDispatch(submitId);
    }
}
