package com.anishan.content.job;

import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.api.event.FixedEventRoutes;
import com.anishan.content.config.CommunityWorkerProperties;
import com.anishan.content.domain.entity.ContentEventOutbox;
import com.anishan.content.service.ContentEventOutboxService;
import com.anishan.content.service.SolutionDomainMigrationGate;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Claims a bounded content outbox batch, commits it, then publishes outside DB transactions. */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "oj.content-community-outbox", name = "enabled", havingValue = "true")
public class ContentOutboxRelay {

    private final ContentEventOutboxService outboxService;
    private final RabbitTemplate rabbitTemplate;
    private final MessageConverter messageConverter;
    private final ObjectMapper objectMapper;
    private final CommunityWorkerProperties properties;
    private final ThreadPoolTaskExecutor publisherExecutor;
    private final SolutionDomainMigrationGate migrationGate;
    private final String leaseOwner = UUID.randomUUID().toString();
    private final AtomicBoolean cycleActive = new AtomicBoolean(false);
    private final AtomicLong lastUnsupportedAlertAt = new AtomicLong(0L);

    public ContentOutboxRelay(ContentEventOutboxService outboxService,
                              @org.springframework.beans.factory.annotation.Qualifier("contentCommunityRabbitTemplate")
                              RabbitTemplate rabbitTemplate,
                              MessageConverter messageConverter,
                              ObjectMapper objectMapper,
                              CommunityWorkerProperties properties,
                              @org.springframework.beans.factory.annotation.Qualifier("contentCommunityPublisherExecutor")
                              ThreadPoolTaskExecutor publisherExecutor,
                              SolutionDomainMigrationGate migrationGate) {
        this.outboxService = outboxService;
        this.rabbitTemplate = rabbitTemplate;
        this.messageConverter = messageConverter;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.publisherExecutor = publisherExecutor;
        this.migrationGate = migrationGate;
    }

    @Scheduled(fixedDelayString = "${oj.content-community-outbox.poll-interval-ms:1000}")
    public void poll() {
        if (!cycleActive.compareAndSet(false, true)) {
            return;
        }

        List<ContentEventOutbox> claimed;
        try {
            if (!migrationGate.isCutover()) {
                cycleActive.set(false);
                return;
            }
            alertUnsupportedTypes();
            claimed = outboxService.claimBatch(leaseOwner, properties.getBatchSize());
        } catch (RuntimeException failure) {
            cycleActive.set(false);
            log.error("Content community outbox poll failed stage=CLAIM errorType={}",
                    failure.getClass().getSimpleName());
            return;
        }

        if (claimed == null || claimed.isEmpty()) {
            cycleActive.set(false);
            return;
        }
        AtomicInteger remaining = new AtomicInteger(claimed.size());
        for (ContentEventOutbox event : claimed) {
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
        long now = System.currentTimeMillis();
        long previous = lastUnsupportedAlertAt.get();
        if (now - previous < TimeUnit.MINUTES.toMillis(1)
                || !lastUnsupportedAlertAt.compareAndSet(previous, now)) {
            return;
        }
        try {
            List<ContentEventOutbox> unsupported = outboxService.findUnsupportedDueEvents(20);
            if (unsupported == null) {
                return;
            }
            for (ContentEventOutbox event : unsupported) {
                if (event != null) {
                    log.error("Unsupported content outbox event eventId={} eventType={} action=NOT_SENT",
                            event.getEventId(), event.getEventType());
                }
            }
        } catch (RuntimeException failure) {
            log.error("Content outbox unsupported-type scan failed errorType={}",
                    failure.getClass().getSimpleName());
        }
    }

    private void publishOne(ContentEventOutbox row) {
        Message message;
        String exchange;
        String routingKey;
        try {
            message = toMessage(row);
            exchange = row.getExchangeName();
            routingKey = row.getRoutingKey();
        } catch (Exception invalid) {
            log.error("Content outbox event rejected eventId={} eventType={} stage=VALIDATE errorType={}",
                    idOf(row), typeOf(row), invalid.getClass().getSimpleName());
            markFailed(row, "INVALID_PAYLOAD");
            return;
        }

        CorrelationData correlationData = new CorrelationData(row.getEventId());
        try {
            rabbitTemplate.send(exchange, routingKey, message, correlationData);
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(properties.getConfirmTimeoutSeconds(), TimeUnit.SECONDS);
            if (correlationData.getReturned() != null) {
                markFailed(row, "MESSAGE_RETURNED");
                return;
            }
            if (confirm == null || !confirm.isAck()) {
                markFailed(row, "BROKER_NACK");
                return;
            }
        } catch (TimeoutException timeout) {
            markFailed(row, "CONFIRM_TIMEOUT");
            return;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            markFailed(row, "PUBLISH_INTERRUPTED");
            return;
        } catch (Exception publishFailure) {
            log.error("Content outbox publish failed eventId={} eventType={} stage=PUBLISH errorType={}",
                    idOf(row), typeOf(row), publishFailure.getClass().getSimpleName());
            markFailed(row, "PUBLISH_FAILURE");
            return;
        }

        try {
            if (!outboxService.markSent(row.getEventId(), leaseOwner)) {
                log.warn("Content outbox confirm lost lease eventId={} eventType={} stage=MARK_SENT",
                        idOf(row), typeOf(row));
            }
        } catch (RuntimeException databaseFailure) {
            // Broker accepted the stable eventId. Let the lease expire and redeliver at-least-once.
            log.error("Content outbox sent-state update failed eventId={} eventType={} stage=MARK_SENT errorType={}",
                    idOf(row), typeOf(row), databaseFailure.getClass().getSimpleName());
        }
    }

    private Message toMessage(ContentEventOutbox row) throws Exception {
        if (row == null || row.getEventId() == null || row.getPayload() == null) {
            throw new IllegalArgumentException("Missing outbox event data");
        }
        CommunityEvent event = CommunityEvent.fromJson(
                row.getPayload().getBytes(StandardCharsets.UTF_8), objectMapper);
        UUID.fromString(row.getEventId());
        CommunityEventType eventType = event.getEventType();
        String expectedRoutingKey = FixedEventRoutes.communityRoutingKey(eventType);
        if (!row.getEventId().equals(event.getEventId())
                || !row.getDedupeKey().equals(event.getDedupeKey())
                || !row.getEventType().equals(eventType.name())
                || !FixedEventRoutes.COMMUNITY_EXCHANGE.equals(row.getExchangeName())
                || !expectedRoutingKey.equals(row.getRoutingKey())) {
            throw new IllegalArgumentException("Content outbox event identity or route mismatch");
        }

        MessageProperties properties = new MessageProperties();
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        Message message = messageConverter.toMessage(event, properties);
        FixedEventRoutes.validatePayloadSize(message.getBody());
        FixedEventRoutes.setMessageId(message, event.getEventId());
        return message;
    }

    private void markFailed(ContentEventOutbox event, String errorCode) {
        try {
            if (!outboxService.markFailed(event, leaseOwner, errorCode)) {
                log.warn("Content outbox failure update lost lease eventId={} eventType={} code={}",
                        idOf(event), typeOf(event), errorCode);
            }
        } catch (RuntimeException updateFailure) {
            log.error("Content outbox failure update failed eventId={} eventType={} stage=MARK_FAILED errorType={}",
                    idOf(event), typeOf(event), updateFailure.getClass().getSimpleName());
        }
    }

    private static String idOf(ContentEventOutbox event) {
        return event == null ? null : event.getEventId();
    }

    private static String typeOf(ContentEventOutbox event) {
        return event == null ? null : event.getEventType();
    }
}
