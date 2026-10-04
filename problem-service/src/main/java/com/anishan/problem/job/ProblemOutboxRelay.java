package com.anishan.problem.job;

import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.event.FixedEventRoutes;
import com.anishan.api.event.PointAwardEvent;
import com.anishan.problem.config.ProblemMessagingConfig;
import com.anishan.problem.config.ProblemOutboxProperties;
import com.anishan.problem.domain.entity.ProblemEventOutbox;
import com.anishan.problem.service.ProblemEventOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "oj.problem-outbox", name = "enabled", havingValue = "true")
public class ProblemOutboxRelay {

    private final ProblemEventOutboxService outboxService;
    private final RabbitTemplate rabbitTemplate;
    private final MessageConverter messageConverter;
    private final ObjectMapper objectMapper;
    private final ProblemOutboxProperties properties;
    private final ThreadPoolTaskExecutor publisherExecutor;
    private final Clock clock;
    private final String leaseOwner = UUID.randomUUID().toString();
    private final AtomicBoolean cycleActive = new AtomicBoolean(false);
    private volatile long lastUnsupportedAlertAt;

    public ProblemOutboxRelay(ProblemEventOutboxService outboxService,
                              @org.springframework.beans.factory.annotation.Qualifier("problemOutboxRabbitTemplate") RabbitTemplate rabbitTemplate,
                              MessageConverter messageConverter,
                              ObjectMapper objectMapper,
                              ProblemOutboxProperties properties,
                              @org.springframework.beans.factory.annotation.Qualifier("problemOutboxPublisherExecutor") ThreadPoolTaskExecutor publisherExecutor,
                              @org.springframework.beans.factory.annotation.Qualifier("problemOutboxClock") Clock clock) {
        this.outboxService = outboxService;
        this.rabbitTemplate = rabbitTemplate;
        this.messageConverter = messageConverter;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.publisherExecutor = publisherExecutor;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${oj.problem-outbox.poll-interval-ms:1000}")
    public void poll() {
        if (!cycleActive.compareAndSet(false, true)) {
            return;
        }
        List<ProblemEventOutbox> claimed;
        try {
            alertUnsupportedTypes();
            claimed = outboxService.claimBatch(leaseOwner, properties.getBatchSize());
        } catch (RuntimeException failure) {
            cycleActive.set(false);
            log.error("Problem outbox claim failed stage=CLAIM errorType={}", failure.getClass().getSimpleName());
            return;
        }
        if (claimed == null || claimed.isEmpty()) {
            cycleActive.set(false);
            return;
        }

        AtomicInteger remaining = new AtomicInteger(claimed.size());
        for (ProblemEventOutbox event : claimed) {
            try {
                publisherExecutor.execute(() -> {
                    try {
                        publishOne(event);
                    } finally {
                        if (remaining.decrementAndGet() == 0) {
                            cycleActive.set(false);
                        }
                    }
                });
            } catch (RejectedExecutionException rejected) {
                markFailed(event, "PUBLISHER_BUSY");
                if (remaining.decrementAndGet() == 0) {
                    cycleActive.set(false);
                }
            }
        }
    }

    private void alertUnsupportedTypes() {
        long now = clock.millis();
        if (now - lastUnsupportedAlertAt < TimeUnit.MINUTES.toMillis(1)) {
            return;
        }
        lastUnsupportedAlertAt = now;
        try {
            List<ProblemEventOutbox> unsupported = outboxService.findUnsupportedDueEvents(20);
            if (unsupported == null) {
                return;
            }
            for (ProblemEventOutbox event : unsupported) {
                if (event != null) {
                    log.error("Unsupported problem outbox event eventId={} eventType={} action=NOT_SENT",
                            event.getEventId(), event.getEventType());
                }
            }
        } catch (RuntimeException failure) {
            log.error("Problem outbox unsupported-type scan failed errorType={}",
                    failure.getClass().getSimpleName());
        }
    }

    private void publishOne(ProblemEventOutbox event) {
        Message message;
        String exchange;
        String routingKey;
        try {
            message = toMessage(event);
            exchange = event.getExchangeName();
            routingKey = event.getRoutingKey();
        } catch (Exception invalid) {
            log.error("Problem outbox event rejected eventId={} eventType={} stage=VALIDATE errorType={}",
                    idOf(event), typeOf(event), invalid.getClass().getSimpleName());
            markFailed(event, "INVALID_PAYLOAD");
            return;
        }

        CorrelationData correlationData = new CorrelationData(event.getEventId());
        try {
            rabbitTemplate.send(exchange, routingKey, message, correlationData);
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(properties.getConfirmTimeoutSeconds(), TimeUnit.SECONDS);
            if (correlationData.getReturned() != null) {
                markFailed(event, "MESSAGE_RETURNED");
                return;
            }
            if (confirm == null || !confirm.isAck()) {
                markFailed(event, "BROKER_NACK");
                return;
            }
        } catch (TimeoutException timeout) {
            markFailed(event, "CONFIRM_TIMEOUT");
            return;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            markFailed(event, "PUBLISH_INTERRUPTED");
            return;
        } catch (Exception publishFailure) {
            log.error("Problem outbox publish failed eventId={} eventType={} stage=PUBLISH errorType={}",
                    idOf(event), typeOf(event), publishFailure.getClass().getSimpleName());
            markFailed(event, "PUBLISH_FAILURE");
            return;
        }

        try {
            if (!outboxService.markSent(event.getEventId(), leaseOwner)) {
                log.warn("Problem outbox confirm lost lease eventId={} eventType={} stage=MARK_SENT",
                        idOf(event), typeOf(event));
            }
        } catch (RuntimeException databaseFailure) {
            // The broker may already have accepted this event. Leave the lease to expire and
            // redeliver with the same eventId rather than recording a false publish failure.
            log.error("Problem outbox sent-state update failed eventId={} eventType={} stage=MARK_SENT errorType={}",
                    idOf(event), typeOf(event), databaseFailure.getClass().getSimpleName());
        }
    }

    private Message toMessage(ProblemEventOutbox event) throws Exception {
        if (event == null || event.getEventId() == null || event.getPayload() == null) {
            throw new IllegalArgumentException("Missing outbox event data");
        }
        UUID.fromString(event.getEventId());
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        Message message;
        if (ProblemEventOutboxService.POINTS_AWARDED.equals(event.getEventType())) {
            requireRoute(event, FixedEventRoutes.POINTS_EXCHANGE, FixedEventRoutes.POINTS_ROUTING_KEY);
            PointAwardEvent points = PointAwardEvent.fromJson(
                    event.getPayload().getBytes(StandardCharsets.UTF_8), objectMapper);
            if (!event.getEventId().equals(points.getEventId())
                    || !event.getDedupeKey().equals(points.getDedupeKey())) {
                throw new IllegalArgumentException("Outbox points identity mismatch");
            }
            message = messageConverter.toMessage(points, properties);
            if (message.getBody().length > FixedEventRoutes.MAX_EVENT_PAYLOAD_BYTES) {
                throw new IllegalArgumentException("Outbox points payload too large");
            }
        } else if (ProblemEventOutboxService.JUDGE_DISPATCH.equals(event.getEventType())) {
            requireRoute(event, ProblemMessagingConfig.JUDGE_EXCHANGE, ProblemMessagingConfig.JUDGE_ROUTING_KEY);
            JudgeInfo judgeInfo = objectMapper.readValue(event.getPayload(), JudgeInfo.class);
            validateJudgeInfo(judgeInfo);
            if (!event.getDedupeKey().equals("judge-dispatch:" + judgeInfo.getSubmitId())) {
                throw new IllegalArgumentException("Outbox judge dedupe mismatch");
            }
            message = messageConverter.toMessage(judgeInfo, properties);
            if (message.getBody().length > ProblemEventOutboxService.MAX_JUDGE_PAYLOAD_BYTES) {
                throw new IllegalArgumentException("Outbox judge payload too large");
            }
        } else {
            throw new IllegalArgumentException("Unsupported problem outbox event type");
        }
        message.getMessageProperties().setMessageId(event.getEventId());
        return message;
    }

    private static void requireRoute(ProblemEventOutbox event, String exchange, String routingKey) {
        if (!exchange.equals(event.getExchangeName()) || !routingKey.equals(event.getRoutingKey())) {
            throw new IllegalArgumentException("Outbox route mismatch");
        }
    }

    private static void validateJudgeInfo(JudgeInfo value) {
        if (value == null || !positive(value.getSubmitId()) || !positive(value.getUserId())
                || !positive(value.getProblemId()) || !positive(value.getLanguageId())
                || value.getCode() == null || value.getLanguage() == null || value.getLanguage().trim().isEmpty()
                || (value.getContestId() != null && !positive(value.getContestId()))) {
            throw new IllegalArgumentException("Invalid judge dispatch payload");
        }
    }

    private static boolean positive(Long value) {
        return value != null && value > 0;
    }

    private void markFailed(ProblemEventOutbox event, String errorCode) {
        try {
            if (!outboxService.markFailed(event, leaseOwner, errorCode)) {
                log.warn("Problem outbox failure update lost lease eventId={} eventType={} code={}",
                        idOf(event), typeOf(event), errorCode);
            }
        } catch (RuntimeException updateFailure) {
            log.error("Problem outbox failure update failed eventId={} eventType={} stage=MARK_FAILED errorType={}",
                    idOf(event), typeOf(event), updateFailure.getClass().getSimpleName());
        }
    }

    private static String idOf(ProblemEventOutbox event) {
        return event == null ? null : event.getEventId();
    }

    private static String typeOf(ProblemEventOutbox event) {
        return event == null ? null : event.getEventType();
    }

}
