package com.anishan.problem.service;

import com.anishan.api.event.PointAwardEvent;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.config.JudgeConfig;
import com.anishan.problem.mapper.ProblemCompleteMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Persists the first-AC marker and its idempotent points event in the same problem DB transaction. */
@Service
public class ProblemCompletionAwardService {

    private final ProblemCompleteMapper problemCompleteMapper;
    private final ProblemEventOutboxService outboxService;
    private final JudgeConfig judgeConfig;
    private final Clock clock;

    public ProblemCompletionAwardService(ProblemCompleteMapper problemCompleteMapper,
                                         ProblemEventOutboxService outboxService,
                                         JudgeConfig judgeConfig,
                                         @Qualifier("contestSubmissionClock") Clock clock) {
        this.problemCompleteMapper = problemCompleteMapper;
        this.outboxService = outboxService;
        this.judgeConfig = judgeConfig;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean completeOnAc(Long userId, Long problemId, BigDecimal score) {
        if (userId == null || userId < 0 || problemId == null || problemId <= 0) {
            throw new ApiStatusException(400, "题目完成信息无效");
        }
        int inserted = problemCompleteMapper.insertIgnore(userId, problemId);
        if (inserted != 1) {
            return false;
        }

        // Virtual account 0 is allowed to receive an AC projection but never earns user points.
        if (userId == 0) {
            return true;
        }
        BigDecimal amount = Boolean.TRUE.equals(judgeConfig.getIsFixedAwardPoint())
                ? judgeConfig.getAwardPoint()
                : (score == null ? BigDecimal.ZERO : score);
        amount = amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_DOWN);
        if (amount.signum() <= 0) {
            return true;
        }

        String userIdValue = userId.toString();
        String problemIdValue = problemId.toString();
        String eventId = UUID.randomUUID().toString();
        PointAwardEvent event = new PointAwardEvent()
                .setSchemaVersion(1)
                .setEventId(eventId)
                .setDedupeKey("points-awarded:" + userIdValue + ":" + problemIdValue)
                .setOccurredAt(OffsetDateTime.now(clock).toString())
                .setUserId(userIdValue)
                .setProblemId(problemIdValue)
                .setAmount(amount.toPlainString());
        outboxService.recordPointAward(event);
        return true;
    }
}
