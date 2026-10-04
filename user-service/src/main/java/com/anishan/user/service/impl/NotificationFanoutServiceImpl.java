package com.anishan.user.service.impl;

import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.user.config.NotificationWorkerProperties;
import com.anishan.user.domain.entity.NotificationFanoutJob;
import com.anishan.user.domain.entity.UserNotification;
import com.anishan.user.domain.enumeration.NotificationFanoutStatus;
import com.anishan.user.mapper.NotificationFanoutJobMapper;
import com.anishan.user.mapper.UserFollowMapper;
import com.anishan.user.mapper.UserNotificationMapper;
import com.anishan.user.service.CommunityEventFreshness;
import com.anishan.user.service.NotificationFanoutService;
import com.anishan.commons.exception.ApiStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class NotificationFanoutServiceImpl implements NotificationFanoutService {

    private static final int MAX_ATTEMPTS = 20;
    private static final long INITIAL_RETRY_SECONDS = 5L;
    private static final long MAX_RETRY_SECONDS = 300L;
    private static final ZoneId STORAGE_ZONE = ZoneId.of("Asia/Shanghai");

    private final NotificationFanoutJobMapper jobMapper;
    private final UserFollowMapper followMapper;
    private final UserNotificationMapper notificationMapper;
    private final NotificationWorkerProperties properties;
    private final ObjectMapper objectMapper;
    private final CommunityEventFreshness freshness;
    private final Clock clock;

    public NotificationFanoutServiceImpl(NotificationFanoutJobMapper jobMapper,
                                         UserFollowMapper followMapper,
                                         UserNotificationMapper notificationMapper,
                                         NotificationWorkerProperties properties,
                                         ObjectMapper objectMapper,
                                         CommunityEventFreshness freshness,
                                         @org.springframework.beans.factory.annotation.Qualifier("userCommunityClock")
                                         Clock clock) {
        this.jobMapper = jobMapper;
        this.followMapper = followMapper;
        this.notificationMapper = notificationMapper;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.freshness = freshness;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(CommunityEvent event) {
        if (event == null || event.getEventType() != CommunityEventType.SOLUTION_PUBLISHED) {
            throw new IllegalArgumentException("Only solution-published events can create fanout jobs");
        }
        event.validate(objectMapper);
        LocalDateTime now = LocalDateTime.now(clock);
        NotificationFanoutJob job = new NotificationFanoutJob();
        job.setEventId(event.getEventId());
        job.setActorId(Long.parseLong(event.getActorId()));
        job.setSolutionId(Long.parseLong(event.getSolutionId()));
        job.setOccurredAt(toStorageTime(event.getOccurredAt()));
        job.setCursorUserId(0L);
        job.setStatus(NotificationFanoutStatus.PENDING);
        job.setAttempts(0);
        job.setNextAttemptAt(now);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        jobMapper.insertIfAbsent(job);

        NotificationFanoutJob persisted = jobMapper.selectByEventIdForUpdate(event.getEventId());
        if (persisted == null || !Objects.equals(persisted.getActorId(), job.getActorId())
                || !Objects.equals(persisted.getSolutionId(), job.getSolutionId())
                || !Objects.equals(persisted.getOccurredAt(), job.getOccurredAt())) {
            log.error("Fanout event identity conflict eventId={} eventType={} code=EVENT_ID_CONFLICT",
                    event.getEventId(), event.getEventType().name());
            throw new IllegalStateException("Fanout event identity conflict");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationFanoutJob claimOne(String leaseOwner) {
        validateLeaseOwner(leaseOwner);
        LocalDateTime now = LocalDateTime.now(clock);
        jobMapper.failExhaustedExpiredClaims(now);
        List<NotificationFanoutJob> due = jobMapper.selectClaimable(now, 1);
        if (due == null || due.isEmpty()) {
            return null;
        }
        NotificationFanoutJob job = due.get(0);
        LocalDateTime leaseUntil = now.plusSeconds(properties.getLeaseSeconds());
        if (jobMapper.markClaimed(job.getEventId(), leaseOwner, leaseUntil, now) != 1) {
            return null;
        }
        job.setStatus(NotificationFanoutStatus.SENDING);
        job.setAttempts((job.getAttempts() == null ? 0 : job.getAttempts()) + 1);
        job.setLeaseOwner(leaseOwner);
        job.setLeaseUntil(leaseUntil);
        job.setLastError(null);
        return job;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processOneBatch(NotificationFanoutJob claimedJob) {
        if (claimedJob == null) {
            throw new IllegalArgumentException("Claimed fanout job is required");
        }
        String leaseOwner = claimedJob.getLeaseOwner();
        validateLeaseOwner(leaseOwner);
        LocalDateTime now = LocalDateTime.now(clock);
        NotificationFanoutJob locked = jobMapper.selectByEventIdForUpdate(claimedJob.getEventId());
        if (!ownsLiveLease(locked, leaseOwner, now)) {
            throw new IllegalStateException("Fanout lease was lost");
        }

        if (freshness.isExpired(locked.getOccurredAt())) {
            freshness.recordExpired(locked.getEventId(), CommunityEventType.SOLUTION_PUBLISHED.name());
            finish(locked, leaseOwner, locked.getCursorUserId(), "DONE", now);
            return;
        }

        List<Long> followerIds = followMapper.selectActiveFollowersForFanout(
                locked.getActorId(), locked.getCursorUserId(), locked.getOccurredAt(),
                properties.getFanoutBatchSize());
        if (followerIds == null) {
            followerIds = Collections.emptyList();
        }

        for (Long followerId : followerIds) {
            UserNotification notification = new UserNotification();
            notification.setRecipientId(followerId);
            notification.setActorId(locked.getActorId());
            notification.setEventId(locked.getEventId());
            notification.setDedupeKey("solution-published:" + locked.getSolutionId());
            notification.setType(CommunityEventType.SOLUTION_PUBLISHED.name());
            notification.setSolutionId(locked.getSolutionId());
            notification.setOccurredAt(locked.getOccurredAt());
            notification.setCreatedAt(now);
            notification.setReadAt(null);
            insertOnce(notification);
        }

        Long nextCursor = followerIds.isEmpty() ? locked.getCursorUserId()
                : followerIds.get(followerIds.size() - 1);
        String nextStatus = followerIds.size() == properties.getFanoutBatchSize() ? "PENDING" : "DONE";
        finish(locked, leaseOwner, nextCursor, nextStatus, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markFailed(NotificationFanoutJob claimedJob, String errorCode) {
        if (claimedJob == null) {
            throw new IllegalArgumentException("Claimed fanout job is required");
        }
        String leaseOwner = claimedJob.getLeaseOwner();
        validateLeaseOwner(leaseOwner);
        LocalDateTime now = LocalDateTime.now(clock);
        int attempts = claimedJob.getAttempts() == null ? 0 : claimedJob.getAttempts();
        boolean terminal = attempts >= MAX_ATTEMPTS;
        LocalDateTime next = terminal ? now : now.plusSeconds(retryDelaySeconds(attempts));
        return jobMapper.markFailed(claimedJob.getEventId(), leaseOwner,
                terminal ? "FAILED" : "PENDING", next, safeErrorCode(errorCode), now) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replayFailed(String eventId) {
        validateEventId(eventId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (jobMapper.replayFailed(eventId, now) == 1) {
            return;
        }
        if (jobMapper.selectStatusByEventId(eventId) == null) {
            throw new ApiStatusException(404, "失败的通知任务不存在");
        }
        throw new ApiStatusException(409, "通知任务当前状态不允许重放");
    }

    private void insertOnce(UserNotification notification) {
        try {
            if (notificationMapper.insertNotification(notification) != 1) {
                throw new IllegalStateException("Notification insert did not create a row");
            }
        } catch (DuplicateKeyException duplicate) {
            Long existingId = notificationMapper.selectIdByRecipientAndDedupeForUpdate(
                    notification.getRecipientId(), notification.getDedupeKey());
            if (existingId != null) {
                return;
            }
            log.error("Fanout notification uniqueness conflict recipientId={} eventId={} code=UNIQUE_KEY_CONFLICT",
                    notification.getRecipientId(), notification.getEventId());
            throw new IllegalStateException("Fanout notification uniqueness conflict");
        }
    }

    private void finish(NotificationFanoutJob job, String leaseOwner, Long cursor,
                        String status, LocalDateTime now) {
        if (jobMapper.finishBatch(job.getEventId(), leaseOwner, cursor, status, now) != 1) {
            throw new IllegalStateException("Fanout lease expired before batch commit");
        }
    }

    private static boolean ownsLiveLease(NotificationFanoutJob job, String owner, LocalDateTime now) {
        return job != null && job.getStatus() == NotificationFanoutStatus.SENDING
                && Objects.equals(job.getLeaseOwner(), owner)
                && job.getLeaseUntil() != null && job.getLeaseUntil().isAfter(now);
    }

    private static LocalDateTime toStorageTime(String offsetTimestamp) {
        return OffsetDateTime.parse(offsetTimestamp).atZoneSameInstant(STORAGE_ZONE).toLocalDateTime();
    }

    private static long retryDelaySeconds(int attempts) {
        long delay = INITIAL_RETRY_SECONDS;
        for (int attempt = 1; attempt < attempts && delay < MAX_RETRY_SECONDS; attempt++) {
            delay = Math.min(MAX_RETRY_SECONDS, delay * 2L);
        }
        return delay;
    }

    private static String safeErrorCode(String errorCode) {
        return errorCode != null && errorCode.matches("[A-Z0-9_]{1,64}")
                ? errorCode : "FANOUT_FAILURE";
    }

    private static void validateLeaseOwner(String leaseOwner) {
        if (leaseOwner == null || leaseOwner.trim().isEmpty() || leaseOwner.length() > 64) {
            throw new IllegalArgumentException("Invalid fanout lease owner");
        }
    }

    private static void validateEventId(String eventId) {
        try {
            com.anishan.api.event.CommunityEvent.validateUuid(eventId);
        } catch (IllegalArgumentException invalid) {
            throw new ApiStatusException(400, "事件ID格式不合法");
        }
    }
}
