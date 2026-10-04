package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.ProblemEventOutbox;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** Outbox persistence; relay claim/lease operations are explicit XML SQL, not generic updates. */
public interface ProblemEventOutboxMapper extends BaseMapper<ProblemEventOutbox> {

    ProblemEventOutbox selectByDedupeKey(@Param("dedupeKey") String dedupeKey);

    ProblemEventOutbox selectByDedupeKeyForUpdate(@Param("dedupeKey") String dedupeKey);

    int retryFailedJudgeDispatch(@Param("eventId") String eventId,
                                 @Param("dedupeKey") String dedupeKey,
                                 @Param("now") LocalDateTime now);

    List<ProblemEventOutbox> selectClaimable(@Param("now") LocalDateTime now,
                                              @Param("limit") int limit);

    int markClaimed(@Param("eventId") String eventId,
                    @Param("leaseOwner") String leaseOwner,
                    @Param("now") LocalDateTime now,
                    @Param("leaseUntil") LocalDateTime leaseUntil);

    int markSent(@Param("eventId") String eventId,
                 @Param("leaseOwner") String leaseOwner,
                 @Param("now") LocalDateTime now);

    int markFailed(@Param("eventId") String eventId,
                   @Param("leaseOwner") String leaseOwner,
                   @Param("status") String status,
                   @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
                   @Param("errorCode") String errorCode,
                   @Param("now") LocalDateTime now);

    int failExhaustedExpiredClaims(@Param("now") LocalDateTime now,
                                   @Param("limit") int limit);

    int deleteExpiredSent(@Param("pointsBefore") LocalDateTime pointsBefore,
                          @Param("judgeBefore") LocalDateTime judgeBefore,
                          @Param("limit") int limit);

    List<ProblemEventOutbox> selectUnsupportedDue(@Param("now") LocalDateTime now,
                                                   @Param("limit") int limit);
}
