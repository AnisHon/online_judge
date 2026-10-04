package com.anishan.user.service.impl;

import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.user.domain.entity.UserNotification;
import com.anishan.user.mapper.SysUserMapper;
import com.anishan.user.mapper.UserNotificationMapper;
import com.anishan.user.service.NotificationDeliveryService;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationDeliveryServiceImpl implements NotificationDeliveryService {

    private static final ZoneId STORAGE_ZONE = ZoneId.of("Asia/Shanghai");

    private final UserNotificationMapper notificationMapper;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public NotificationDeliveryServiceImpl(UserNotificationMapper notificationMapper,
                                           SysUserMapper sysUserMapper,
                                           ObjectMapper objectMapper,
                                           @org.springframework.beans.factory.annotation.Qualifier("userCommunityClock")
                                           Clock clock) {
        this.notificationMapper = notificationMapper;
        this.sysUserMapper = sysUserMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliver(CommunityEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Community event is required");
        }
        event.validate(objectMapper);
        CommunityEventType type = event.getEventType();
        if (type == CommunityEventType.SOLUTION_PUBLISHED) {
            throw new IllegalArgumentException("Published events must use fanout registration");
        }

        Set<Long> requested = new LinkedHashSet<>();
        for (String recipientId : event.getRecipientIds()) {
            long parsedId = Long.parseLong(recipientId);
            if (parsedId != Long.parseLong(event.getActorId()) || isModeration(type)) {
                requested.add(parsedId);
            }
        }
        if (requested.isEmpty()) {
            return;
        }

        List<Long> normalUserIds = sysUserMapper.selectNormalUserIds(new ArrayList<>(requested));
        Set<Long> deliverable = normalUserIds == null ? new LinkedHashSet<>()
                : new LinkedHashSet<>(normalUserIds);
        LocalDateTime occurredAt = OffsetDateTime.parse(event.getOccurredAt())
                .atZoneSameInstant(STORAGE_ZONE).toLocalDateTime();
        LocalDateTime createdAt = LocalDateTime.now(clock);
        for (Long recipientId : requested) {
            if (!deliverable.contains(recipientId)) {
                continue;
            }
            UserNotification notification = new UserNotification();
            notification.setRecipientId(recipientId);
            notification.setActorId(Long.parseLong(event.getActorId()));
            notification.setEventId(event.getEventId());
            notification.setDedupeKey(event.getDedupeKey());
            notification.setType(type.name());
            notification.setSolutionId(Long.parseLong(event.getSolutionId()));
            notification.setCommentId(event.getCommentId() == null ? null : Long.parseLong(event.getCommentId()));
            notification.setAction(event.getAction());
            notification.setReason(event.getReason());
            notification.setOccurredAt(occurredAt);
            notification.setCreatedAt(createdAt);
            notification.setReadAt(null);
            insertOnce(notification);
        }
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
                // A replay, including one with a different eventId, must not rewrite read_at.
                return;
            }
            log.error("Notification uniqueness conflict recipientId={} eventId={} type={} code=UNIQUE_KEY_CONFLICT",
                    notification.getRecipientId(), notification.getEventId(), notification.getType());
            throw new IllegalStateException("Notification uniqueness conflict");
        }
    }

    private static boolean isModeration(CommunityEventType type) {
        return type == CommunityEventType.SOLUTION_MODERATED
                || type == CommunityEventType.COMMENT_MODERATED;
    }
}
