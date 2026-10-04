package com.anishan.content.mapper;

import com.anishan.content.domain.entity.ContentEventOutbox;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ContentEventOutboxMapper extends BaseMapper<ContentEventOutbox> {

    ContentEventOutbox selectByDedupeKey(@Param("dedupeKey") String dedupeKey);

    ContentEventOutbox selectByDedupeKeyForUpdate(@Param("dedupeKey") String dedupeKey);

    ContentEventOutbox selectByEventIdForUpdate(@Param("eventId") String eventId);

    int replayCommunityFailed(@Param("eventId") String eventId,
                              @Param("now") LocalDateTime now);

    List<ContentEventOutbox> selectClaimable(@Param("now") LocalDateTime now,
                                             @Param("limit") int limit);

    int markClaimed(@Param("eventId") String eventId,
                    @Param("leaseOwner") String leaseOwner,
                    @Param("now") LocalDateTime now,
                    @Param("leaseUntil") LocalDateTime leaseUntil);

    int failExhaustedExpiredClaims(@Param("now") LocalDateTime now, @Param("limit") int limit);

    int markSent(@Param("eventId") String eventId,
                 @Param("leaseOwner") String leaseOwner,
                 @Param("now") LocalDateTime now);

    int markFailed(@Param("eventId") String eventId,
                   @Param("leaseOwner") String leaseOwner,
                   @Param("status") String status,
                   @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
                   @Param("errorCode") String errorCode,
                   @Param("now") LocalDateTime now);

    List<ContentEventOutbox> selectUnsupportedDue(@Param("now") LocalDateTime now,
                                                  @Param("limit") int limit);

    int deleteExpiredSent(@Param("sentBefore") LocalDateTime sentBefore, @Param("limit") int limit);
}
