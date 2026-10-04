package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.ContestAttempt;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.problem.domain.enumeration.ContestAttemptState;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ContestAttemptMapper extends BaseMapper<ContestAttempt> {
    long countByContestId(@Param("contestId") Long contestId);

    ContestAttempt selectBySubmitIdForUpdate(@Param("submitId") Long submitId);

    int updatePendingAttemptStatus(@Param("submitId") Long submitId,
                                   @Param("status") JudgeResult status);

    int applyPendingAttemptResult(@Param("submitId") Long submitId,
                                  @Param("state") ContestAttemptState state,
                                  @Param("status") JudgeResult status,
                                  @Param("score") BigDecimal score,
                                  @Param("correct") Boolean correct,
                                  @Param("completedAt") LocalDateTime completedAt);

    List<Long> selectUserIdsWithAttempts(@Param("contestId") Long contestId,
                                         @Param("userIds") List<Long> userIds);

    long countPendingByContestId(@Param("contestId") Long contestId);

    List<Long> selectPendingAttemptReferences(@Param("contestId") Long contestId,
                                              @Param("limit") int limit);

    List<Long> selectInconsistentAttemptReferences(@Param("contestId") Long contestId,
                                                   @Param("limit") int limit);

    List<Long> selectUnlinkedUnappliedSubmitIds(@Param("contestId") Long contestId,
                                                @Param("limit") int limit);
}
