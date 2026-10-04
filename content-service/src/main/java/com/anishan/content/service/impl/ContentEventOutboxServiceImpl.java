package com.anishan.content.service.impl;

import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.api.event.FixedEventRoutes;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.config.CommunityWorkerProperties;
import com.anishan.content.domain.entity.ContentEventOutbox;
import com.anishan.content.mapper.ContentEventOutboxMapper;
import com.anishan.content.service.ContentEventOutboxService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Writes community events in the caller's content-service transaction and owns the
 * claim/lease state machine. This service deliberately has no RabbitTemplate dependency.
 */
@Service
@Slf4j
public class ContentEventOutboxServiceImpl implements ContentEventOutboxService {

    public static final int MAX_ATTEMPTS = 20;
    public static final int MAX_CLEANUP_BATCH_SIZE = 500;

    private static final long INITIAL_RETRY_SECONDS = 5L;
    private static final long MAX_RETRY_SECONDS = 300L;
    private static final List<String> ALLOWED_EVENT_TYPES = Collections.unmodifiableList(Arrays.asList(
            CommunityEventType.SOLUTION_PUBLISHED.name(),
            CommunityEventType.SOLUTION_LIKED.name(),
            CommunityEventType.SOLUTION_COMMENTED.name(),
            CommunityEventType.COMMENT_REPLIED.name(),
            CommunityEventType.COMMENT_LIKED.name(),
            CommunityEventType.SOLUTION_MODERATED.name(),
            CommunityEventType.COMMENT_MODERATED.name()));

    private final ContentEventOutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;
    private final MessageConverter messageConverter;
    private final CommunityWorkerProperties properties;
    private final Clock clock;

    public ContentEventOutboxServiceImpl(ContentEventOutboxMapper outboxMapper,
                                         ObjectMapper objectMapper,
                                         MessageConverter messageConverter,
                                         CommunityWorkerProperties properties,
                                         @org.springframework.beans.factory.annotation.Qualifier("contentCommunityOutboxClock")
                                         Clock clock) {
        this.outboxMapper = outboxMapper;
        this.objectMapper = objectMapper;
        this.messageConverter = messageConverter;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String recordCommunity(CommunityEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Community event is required");
        }
        event.validate(objectMapper);
        String eventId = event.getEventId();
        String eventType = event.getEventType().name();
        String routingKey = FixedEventRoutes.communityRoutingKey(event.getEventType());
        Message encoded = messageConverter.toMessage(event, new MessageProperties());
        byte[] payload = encoded.getBody();
        FixedEventRoutes.validatePayloadSize(payload);

        ContentEventOutbox candidate = newPendingEvent(eventId, event.getDedupeKey(), eventType,
                FixedEventRoutes.COMMUNITY_EXCHANGE, routingKey, payload);
        return insertIdempotently(candidate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ContentEventOutbox> claimBatch(String leaseOwner, int limit) {
        validateLeaseOwner(leaseOwner);
        if (limit < 1 || limit > properties.getBatchSize() || limit > 20) {
            throw new IllegalArgumentException("Invalid content outbox claim limit");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        outboxMapper.failExhaustedExpiredClaims(now, limit);
        List<ContentEventOutbox> due = outboxMapper.selectClaimable(now, limit);
        if (due == null || due.isEmpty()) {
            return Collections.emptyList();
        }

        List<ContentEventOutbox> claimed = new ArrayList<>(due.size());
        LocalDateTime leaseUntil = now.plusSeconds(properties.getLeaseSeconds());
        for (ContentEventOutbox row : due) {
            if (row == null || !ALLOWED_EVENT_TYPES.contains(row.getEventType())) {
                continue;
            }
            if (outboxMapper.markClaimed(row.getEventId(), leaseOwner, now, leaseUntil) == 1) {
                row.setStatus("SENDING")
                        .setAttempts((row.getAttempts() == null ? 0 : row.getAttempts()) + 1)
                        .setLeaseOwner(leaseOwner)
                        .setLeaseUntil(leaseUntil)
                        .setLastError(null);
                claimed.add(row);
            }
        }
        return claimed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markSent(String eventId, String leaseOwner) {
        validateLeaseOwner(leaseOwner);
        return outboxMapper.markSent(eventId, leaseOwner, LocalDateTime.now(clock)) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markFailed(ContentEventOutbox event, String leaseOwner, String errorCode) {
        validateLeaseOwner(leaseOwner);
        if (event == null || !ALLOWED_EVENT_TYPES.contains(event.getEventType())) {
            throw new IllegalArgumentException("Unsupported content outbox event");
        }
        int attempts = event.getAttempts() == null ? 0 : event.getAttempts();
        boolean terminal = attempts >= MAX_ATTEMPTS;
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime nextAttemptAt = terminal ? now : now.plusSeconds(retryDelaySeconds(attempts));
        return outboxMapper.markFailed(event.getEventId(), leaseOwner,
                terminal ? "FAILED" : "PENDING", nextAttemptAt, safeErrorCode(errorCode), now) == 1;
    }

    @Override
    public List<ContentEventOutbox> findUnsupportedDueEvents(int limit) {
        if (limit < 1 || limit > 20) {
            throw new IllegalArgumentException("Invalid unsupported content event query limit");
        }
        return outboxMapper.selectUnsupportedDue(LocalDateTime.now(clock), limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupSentBatch() {
        LocalDateTime now = LocalDateTime.now(clock);
        int limit = Math.min(properties.getCleanupBatchSize(), MAX_CLEANUP_BATCH_SIZE);
        return outboxMapper.deleteExpiredSent(now.minusDays(30), limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String replayCommunityFailed(String eventId) {
        try {
            CommunityEvent.validateUuid(eventId);
        } catch (IllegalArgumentException invalid) {
            throw new ApiStatusException(400, "事件ID格式不合法");
        }
        ContentEventOutbox event = outboxMapper.selectByEventIdForUpdate(eventId);
        if (event == null) {
            throw new ApiStatusException(404, "社区事件不存在");
        }
        if (!ALLOWED_EVENT_TYPES.contains(event.getEventType())) {
            throw new ApiStatusException(409, "该事件类型不允许通过社区接口重放");
        }
        if (!"FAILED".equals(event.getStatus())) {
            throw new ApiStatusException(409, "社区事件当前状态不允许重放");
        }
        if (outboxMapper.replayCommunityFailed(eventId, LocalDateTime.now(clock)) != 1) {
            throw new ApiStatusException(409, "社区事件状态已变化，请刷新后重试");
        }
        return event.getEventType();
    }

    private String insertIdempotently(ContentEventOutbox candidate) {
        ContentEventOutbox existing = outboxMapper.selectByDedupeKey(candidate.getDedupeKey());
        if (existing != null) {
            return returnExistingOrFail(candidate, existing);
        }
        try {
            if (outboxMapper.insert(candidate) != 1) {
                throw new IllegalStateException("Content outbox insert did not create a row");
            }
            return candidate.getEventId();
        } catch (DuplicateKeyException duplicate) {
            // Use a current/locking read after a uniqueness race under REPEATABLE_READ.
            ContentEventOutbox concurrent = outboxMapper.selectByDedupeKeyForUpdate(candidate.getDedupeKey());
            if (concurrent == null) {
                log.error("Content outbox uniqueness conflict eventId={} eventType={} code=UNIQUE_KEY_CONFLICT",
                        candidate.getEventId(), candidate.getEventType());
                throw new IllegalStateException("Content outbox uniqueness conflict");
            }
            return returnExistingOrFail(candidate, concurrent);
        }
    }

    private String returnExistingOrFail(ContentEventOutbox candidate, ContentEventOutbox existing) {
        if (sameContent(candidate, existing)) {
            return existing.getEventId();
        }
        log.error("Content outbox dedupe conflict eventId={} eventType={} code=DEDUPE_PAYLOAD_MISMATCH",
                existing.getEventId(), existing.getEventType());
        throw new IllegalStateException("Content outbox dedupe key has different content");
    }

    private boolean sameContent(ContentEventOutbox left, ContentEventOutbox right) {
        if (!Objects.equals(left.getDedupeKey(), right.getDedupeKey())
                || !Objects.equals(left.getEventType(), right.getEventType())
                || !Objects.equals(left.getExchangeName(), right.getExchangeName())
                || !Objects.equals(left.getRoutingKey(), right.getRoutingKey())
                || left.getPayload() == null || right.getPayload() == null) {
            return false;
        }
        try {
            JsonNode leftJson = objectMapper.readTree(left.getPayload());
            JsonNode rightJson = objectMapper.readTree(right.getPayload());
            return Objects.equals(leftJson, rightJson);
        } catch (Exception invalidJson) {
            return false;
        }
    }

    private ContentEventOutbox newPendingEvent(String eventId, String dedupeKey, String eventType,
                                               String exchange, String routingKey, byte[] payload) {
        LocalDateTime now = LocalDateTime.now(clock);
        return new ContentEventOutbox()
                .setEventId(eventId)
                .setDedupeKey(dedupeKey)
                .setEventType(eventType)
                .setExchangeName(exchange)
                .setRoutingKey(routingKey)
                .setPayload(new String(payload, StandardCharsets.UTF_8))
                .setStatus("PENDING")
                .setAttempts(0)
                .setNextAttemptAt(now)
                .setCreatedAt(now);
    }

    private static long retryDelaySeconds(int attempts) {
        long delay = INITIAL_RETRY_SECONDS;
        for (int index = 1; index < attempts && delay < MAX_RETRY_SECONDS; index++) {
            delay = Math.min(MAX_RETRY_SECONDS, delay * 2L);
        }
        return delay;
    }

    private static String safeErrorCode(String errorCode) {
        return errorCode != null && errorCode.matches("[A-Z0-9_]{1,64}")
                ? errorCode : "PUBLISH_FAILURE";
    }

    private static void validateLeaseOwner(String leaseOwner) {
        if (leaseOwner == null || leaseOwner.trim().isEmpty() || leaseOwner.length() > 64) {
            throw new IllegalArgumentException("Invalid content outbox lease owner");
        }
    }
}
