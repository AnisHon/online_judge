package com.anishan.user.mapper;

import com.anishan.user.domain.entity.NotificationFanoutJob;
import com.anishan.user.domain.enumeration.NotificationFanoutStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** Base persistence operations for a fanout job; claiming requires a transaction. */
@Mapper
public interface NotificationFanoutJobMapper {

    int insertIfAbsent(@Param("job") NotificationFanoutJob job);

    NotificationFanoutJob selectByEventIdForUpdate(@Param("eventId") String eventId);

    List<NotificationFanoutJob> selectClaimable(@Param("now") LocalDateTime now,
                                                @Param("limit") Integer limit);

    int markClaimed(@Param("eventId") String eventId,
                    @Param("leaseOwner") String leaseOwner,
                    @Param("leaseUntil") LocalDateTime leaseUntil,
                    @Param("now") LocalDateTime now);

    int failExhaustedExpiredClaims(@Param("now") LocalDateTime now);

    int finishBatch(@Param("eventId") String eventId,
                    @Param("leaseOwner") String leaseOwner,
                    @Param("cursorUserId") Long cursorUserId,
                    @Param("status") String status,
                    @Param("now") LocalDateTime now);

    int markFailed(@Param("eventId") String eventId,
                   @Param("leaseOwner") String leaseOwner,
                   @Param("status") String status,
                   @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
                   @Param("errorCode") String errorCode,
                   @Param("now") LocalDateTime now);

    int replayFailed(@Param("eventId") String eventId, @Param("now") LocalDateTime now);

    NotificationFanoutStatus selectStatusByEventId(@Param("eventId") String eventId);

    int deleteDoneBefore(@Param("completedBefore") LocalDateTime completedBefore,
                         @Param("limit") Integer limit);
}
