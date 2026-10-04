package com.anishan.problem.service.impl;

import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.event.FixedEventRoutes;
import com.anishan.api.event.PointAwardEvent;
import com.anishan.problem.config.ProblemMessagingConfig;
import com.anishan.problem.config.ProblemOutboxProperties;
import com.anishan.problem.domain.entity.ProblemEventOutbox;
import com.anishan.problem.mapper.ProblemEventOutboxMapper;
import com.anishan.problem.service.ProblemEventOutboxService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class ProblemEventOutboxServiceImpl implements ProblemEventOutboxService {

    public static final String POINTS_AWARDED = ProblemEventOutboxService.POINTS_AWARDED;
    public static final String JUDGE_DISPATCH = ProblemEventOutboxService.JUDGE_DISPATCH;
    public static final int MAX_JUDGE_PAYLOAD_BYTES = ProblemEventOutboxService.MAX_JUDGE_PAYLOAD_BYTES;
    public static final int MAX_ATTEMPTS = 20;
    public static final int MAX_CLEANUP_BATCH_SIZE = 500;

    private static final long INITIAL_RETRY_SECONDS = 5L;
    private static final long MAX_RETRY_SECONDS = 300L;
    private static final List<String> ALLOWED_EVENT_TYPES = Collections.unmodifiableList(
            Arrays.asList(POINTS_AWARDED, JUDGE_DISPATCH));

    private final ProblemEventOutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;
    private final MessageConverter messageConverter;
    private final ProblemOutboxProperties properties;
    @org.springframework.beans.factory.annotation.Qualifier("problemOutboxClock")
    private final Clock clock;

    @Override
    @Transactional
    public String recordPointAward(PointAwardEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Points event is required");
        }
        event.validate(objectMapper);
        byte[] payload = serialize(event, FixedEventRoutes.MAX_EVENT_PAYLOAD_BYTES);
        ProblemEventOutbox candidate = newPendingEvent(
                event.getEventId(), event.getDedupeKey(), POINTS_AWARDED,
                FixedEventRoutes.POINTS_EXCHANGE, FixedEventRoutes.POINTS_ROUTING_KEY, payload);
        return insertIdempotently(candidate);
    }

    @Override
    @Transactional
    public String recordJudgeDispatch(JudgeInfo judgeInfo) {
        validateJudgeInfo(judgeInfo);
        byte[] payload = serialize(judgeInfo, MAX_JUDGE_PAYLOAD_BYTES);
        String dedupeKey = "judge-dispatch:" + judgeInfo.getSubmitId();
        ProblemEventOutbox candidate = newPendingEvent(
                UUID.randomUUID().toString(), dedupeKey, JUDGE_DISPATCH,
                ProblemMessagingConfig.JUDGE_EXCHANGE, ProblemMessagingConfig.JUDGE_ROUTING_KEY, payload);
        return insertIdempotently(candidate);
    }

    @Override
    @Transactional
    public List<ProblemEventOutbox> claimBatch(String leaseOwner, int limit) {
        validateLeaseOwner(leaseOwner);
        if (limit < 1 || limit > properties.getBatchSize() || limit > 20) {
            throw new IllegalArgumentException("Invalid outbox claim limit");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        outboxMapper.failExhaustedExpiredClaims(now, limit);
        List<ProblemEventOutbox> due = outboxMapper.selectClaimable(now, limit);
        if (due == null || due.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProblemEventOutbox> claimed = new ArrayList<>(due.size());
        LocalDateTime leaseUntil = now.plusSeconds(properties.getLeaseSeconds());
        for (ProblemEventOutbox row : due) {
            if (row == null || !ALLOWED_EVENT_TYPES.contains(row.getEventType())) {
                continue;
            }
            int changed = outboxMapper.markClaimed(row.getEventId(), leaseOwner, now, leaseUntil);
            if (changed == 1) {
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
    @Transactional
    public boolean markSent(String eventId, String leaseOwner) {
        validateLeaseOwner(leaseOwner);
        return outboxMapper.markSent(eventId, leaseOwner, LocalDateTime.now(clock)) == 1;
    }

    @Override
    @Transactional
    public boolean markFailed(ProblemEventOutbox claimedEvent, String leaseOwner, String errorCode) {
        validateLeaseOwner(leaseOwner);
        if (claimedEvent == null || !ALLOWED_EVENT_TYPES.contains(claimedEvent.getEventType())) {
            throw new IllegalArgumentException("Unsupported problem outbox event");
        }
        int attempts = claimedEvent.getAttempts() == null ? 0 : claimedEvent.getAttempts();
        boolean terminal = attempts >= MAX_ATTEMPTS;
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime nextAttemptAt = terminal ? now : now.plusSeconds(retryDelaySeconds(attempts));
        return outboxMapper.markFailed(
                claimedEvent.getEventId(), leaseOwner, terminal ? "FAILED" : "PENDING",
                nextAttemptAt, safeErrorCode(errorCode), now) == 1;
    }

    @Override
    @Transactional
    public int cleanupSentBatch() {
        LocalDateTime now = LocalDateTime.now(clock);
        int limit = Math.min(properties.getCleanupBatchSize(), MAX_CLEANUP_BATCH_SIZE);
        return outboxMapper.deleteExpiredSent(now.minusDays(30), now.minusHours(24), limit);
    }

    @Override
    public List<ProblemEventOutbox> findUnsupportedDueEvents(int limit) {
        if (limit < 1 || limit > 20) {
            throw new IllegalArgumentException("Invalid unsupported event query limit");
        }
        return outboxMapper.selectUnsupportedDue(LocalDateTime.now(clock), limit);
    }

    private String insertIdempotently(ProblemEventOutbox candidate) {
        ProblemEventOutbox existing = outboxMapper.selectByDedupeKey(candidate.getDedupeKey());
        if (existing != null) {
            return returnExistingOrFail(candidate, existing);
        }
        try {
            if (outboxMapper.insert(candidate) != 1) {
                throw new IllegalStateException("Problem outbox insert did not create a row");
            }
            return candidate.getEventId();
        } catch (DuplicateKeyException duplicate) {
            // A locking/current read sees the winning insert even under MySQL REPEATABLE_READ,
            // where the earlier plain lookup may have established an older snapshot.
            ProblemEventOutbox concurrent = outboxMapper.selectByDedupeKeyForUpdate(candidate.getDedupeKey());
            if (concurrent == null) {
                log.error("Problem outbox uniqueness conflict eventId={} eventType={} code=UNIQUE_KEY_CONFLICT",
                        candidate.getEventId(), candidate.getEventType());
                throw new IllegalStateException("Problem outbox uniqueness conflict");
            }
            return returnExistingOrFail(candidate, concurrent);
        }
    }

    private String returnExistingOrFail(ProblemEventOutbox candidate, ProblemEventOutbox existing) {
        if (sameContent(candidate, existing)) {
            return existing.getEventId();
        }
        log.error("Problem outbox dedupe conflict eventId={} eventType={} code=DEDUPE_PAYLOAD_MISMATCH",
                existing.getEventId(), existing.getEventType());
        throw new IllegalStateException("Problem outbox dedupe key has different content");
    }

    private boolean sameContent(ProblemEventOutbox left, ProblemEventOutbox right) {
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

    private ProblemEventOutbox newPendingEvent(String eventId, String dedupeKey, String eventType,
                                                String exchange, String routingKey, byte[] payload) {
        LocalDateTime now = LocalDateTime.now(clock);
        return new ProblemEventOutbox()
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

    private byte[] serialize(Object payload, int maxBytes) {
        Message message = messageConverter.toMessage(payload, new MessageProperties());
        byte[] body = message.getBody();
        if (body.length > maxBytes) {
            throw new IllegalArgumentException("Outbox payload exceeds the configured event limit");
        }
        return body;
    }

    private static void validateJudgeInfo(JudgeInfo judgeInfo) {
        if (judgeInfo == null || !positive(judgeInfo.getSubmitId())
                || !positive(judgeInfo.getUserId()) || !positive(judgeInfo.getProblemId())
                || !positive(judgeInfo.getLanguageId()) || judgeInfo.getCode() == null
                || judgeInfo.getLanguage() == null || judgeInfo.getLanguage().trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid judge dispatch payload");
        }
        if (judgeInfo.getContestId() != null && !positive(judgeInfo.getContestId())) {
            throw new IllegalArgumentException("Invalid judge dispatch contest id");
        }
    }

    private static boolean positive(Long value) {
        return value != null && value > 0;
    }

    private static long retryDelaySeconds(int attempts) {
        if (attempts <= 1) {
            return INITIAL_RETRY_SECONDS;
        }
        long delay = INITIAL_RETRY_SECONDS;
        for (int index = 1; index < attempts && delay < MAX_RETRY_SECONDS; index++) {
            delay = Math.min(MAX_RETRY_SECONDS, delay * 2L);
        }
        return delay;
    }

    private static String safeErrorCode(String errorCode) {
        if (errorCode != null && errorCode.matches("[A-Z0-9_]{1,64}")) {
            return errorCode;
        }
        return "PUBLISH_FAILURE";
    }

    private static void validateLeaseOwner(String leaseOwner) {
        if (leaseOwner == null || leaseOwner.trim().isEmpty() || leaseOwner.length() > 64) {
            throw new IllegalArgumentException("Invalid outbox lease owner");
        }
    }
}
