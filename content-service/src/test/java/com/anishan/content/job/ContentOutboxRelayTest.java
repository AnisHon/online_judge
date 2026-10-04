package com.anishan.content.job;

import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.api.event.FixedEventRoutes;
import com.anishan.content.config.CommunityWorkerProperties;
import com.anishan.content.domain.entity.ContentEventOutbox;
import com.anishan.content.service.ContentEventOutboxService;
import com.anishan.content.service.SolutionDomainMigrationGate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContentOutboxRelayTest {

    private final ContentEventOutboxService service = mock(ContentEventOutboxService.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final SolutionDomainMigrationGate migrationGate = mock(SolutionDomainMigrationGate.class);
    private final MessageConverter converter = new Jackson2JsonMessageConverter();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CommunityWorkerProperties properties = new CommunityWorkerProperties();
    private ThreadPoolTaskExecutor executor;
    private ContentOutboxRelay relay;

    @BeforeEach
    void setUp() {
        properties.setConfirmTimeoutSeconds(1);
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.initialize();
        relay = new ContentOutboxRelay(service, rabbitTemplate, converter, objectMapper, properties,
                executor, migrationGate);
        when(migrationGate.isCutover()).thenReturn(true);
        when(service.findUnsupportedDueEvents(20)).thenReturn(Collections.emptyList());
        when(service.markSent(anyString(), anyString())).thenReturn(true);
        when(service.markFailed(any(ContentEventOutbox.class), anyString(), anyString())).thenReturn(true);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdown();
        executor.getThreadPoolExecutor().awaitTermination(3, TimeUnit.SECONDS);
    }

    @Test
    void workerIsOptInAndRemainsDisabledByDefault() {
        assertFalse(new CommunityWorkerProperties().isEnabled());
        ConditionalOnProperty relayCondition = ContentOutboxRelay.class.getAnnotation(ConditionalOnProperty.class);
        ConditionalOnProperty cleanupCondition = ContentOutboxCleanupJob.class.getAnnotation(ConditionalOnProperty.class);
        assertNotNull(relayCondition);
        assertNotNull(cleanupCondition);
        assertEquals("true", relayCondition.havingValue());
        assertEquals("true", cleanupCondition.havingValue());
        assertEquals("oj.content-community-outbox", relayCondition.prefix());
    }

    @Test
    void ackPublishesPersistentFixedRouteMessageWithStableIdAndStringLongIds() throws Exception {
        CommunityEvent event = event();
        ContentEventOutbox row = row(event);
        when(service.claimBatch(anyString(), eq(20))).thenReturn(Collections.singletonList(row));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().set(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(eq(FixedEventRoutes.COMMUNITY_EXCHANGE),
                eq("solution.liked.v1"), any(Message.class), any(CorrelationData.class));

        relay.poll();

        org.mockito.ArgumentCaptor<Message> messageCaptor = org.mockito.ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate, timeout(2000)).send(eq(FixedEventRoutes.COMMUNITY_EXCHANGE),
                eq("solution.liked.v1"), messageCaptor.capture(), any(CorrelationData.class));
        Message sent = messageCaptor.getValue();
        assertEquals(event.getEventId(), sent.getMessageProperties().getMessageId());
        assertEquals(MessageDeliveryMode.PERSISTENT, sent.getMessageProperties().getDeliveryMode());
        JsonNode body = objectMapper.readTree(sent.getBody());
        assertEquals("2090000000000000000", body.get("solutionId").asText());
        assertEquals("100", body.get("actorId").asText());
        assertFalse(body.has("token"));
        assertFalse(body.has("title"));
        assertFalse(body.has("content"));
        verify(service, timeout(2000)).markSent(eq(event.getEventId()), anyString());
        verify(service, never()).markFailed(any(ContentEventOutbox.class), anyString(), anyString());
    }

    @Test
    void brokerNackIsRetriedAndNotMarkedSent() {
        ContentEventOutbox row = row(event());
        when(service.claimBatch(anyString(), eq(20))).thenReturn(Collections.singletonList(row));
        doAnswer(invocation -> {
            ((CorrelationData) invocation.getArgument(3)).getFuture()
                    .set(new CorrelationData.Confirm(false, "broker rejected"));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        relay.poll();

        verify(service, timeout(2000)).markFailed(eq(row), anyString(), eq("BROKER_NACK"));
        verify(service, never()).markSent(anyString(), anyString());
    }

    @Test
    void returnedMessageIsNotMarkedSentEvenWhenBrokerConfirmsIt() {
        ContentEventOutbox row = row(event());
        when(service.claimBatch(anyString(), eq(20))).thenReturn(Collections.singletonList(row));
        doAnswer(invocation -> {
            Message message = invocation.getArgument(2);
            CorrelationData correlation = invocation.getArgument(3);
            correlation.setReturned(new ReturnedMessage(message, 312, "NO_ROUTE",
                    FixedEventRoutes.COMMUNITY_EXCHANGE, "solution.liked.v1"));
            correlation.getFuture().set(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        relay.poll();

        verify(service, timeout(2000)).markFailed(eq(row), anyString(), eq("MESSAGE_RETURNED"));
        verify(service, never()).markSent(anyString(), anyString());
    }

    @Test
    void missingConfirmTimesOutAndRemainsRetryable() {
        ContentEventOutbox row = row(event());
        when(service.claimBatch(anyString(), eq(20))).thenReturn(Collections.singletonList(row));
        doNothing().when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        relay.poll();

        verify(service, timeout(2500)).markFailed(eq(row), anyString(), eq("CONFIRM_TIMEOUT"));
        verify(service, never()).markSent(anyString(), anyString());
    }

    @Test
    void sentDatabaseFailureLeavesLeaseForAtLeastOnceRecovery() {
        ContentEventOutbox row = row(event());
        when(service.claimBatch(anyString(), eq(20))).thenReturn(Collections.singletonList(row));
        when(service.markSent(eq(row.getEventId()), anyString())).thenThrow(new IllegalStateException("database"));
        doAnswer(invocation -> {
            ((CorrelationData) invocation.getArgument(3)).getFuture()
                    .set(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        relay.poll();

        verify(service, timeout(2000)).markSent(eq(row.getEventId()), anyString());
        verify(service, never()).markFailed(any(ContentEventOutbox.class), anyString(), anyString());
    }

    @Test
    void doesNotClaimAnotherBatchWhileCurrentBatchIsWaitingForConfirm() throws Exception {
        ContentEventOutbox row = row(event());
        CountDownLatch sendStarted = new CountDownLatch(1);
        AtomicReference<CorrelationData> sentCorrelation = new AtomicReference<>();
        when(service.claimBatch(anyString(), eq(20))).thenReturn(Collections.singletonList(row));
        doAnswer(invocation -> {
            sentCorrelation.set(invocation.getArgument(3));
            sendStarted.countDown();
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        relay.poll();
        assertTrue(sendStarted.await(1, TimeUnit.SECONDS));

        relay.poll();

        verify(service, times(1)).claimBatch(anyString(), eq(20));
        sentCorrelation.get().getFuture().set(new CorrelationData.Confirm(true, null));
        verify(service, timeout(2000)).markSent(eq(row.getEventId()), anyString());
    }

    @Test
    void preCutoverWorkerDoesNotClaimOrPublish() {
        when(migrationGate.isCutover()).thenReturn(false);

        relay.poll();

        verify(migrationGate).isCutover();
        verifyNoInteractions(service, rabbitTemplate);
    }

    @Test
    void unsupportedRouteOrCrossDomainTypeIsNeverPublished() {
        ContentEventOutbox row = row(event()).setExchangeName("account.points.v1");
        when(service.claimBatch(anyString(), eq(20))).thenReturn(Collections.singletonList(row));

        relay.poll();

        verify(service, timeout(2000)).markFailed(eq(row), anyString(), eq("INVALID_PAYLOAD"));
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void cleanupAlsoRequiresCutover() {
        when(migrationGate.isCutover()).thenReturn(false);
        ContentOutboxCleanupJob cleanup = new ContentOutboxCleanupJob(service, migrationGate);

        cleanup.cleanup();

        verify(migrationGate).isCutover();
        verify(service, never()).cleanupSentBatch();
    }

    private ContentEventOutbox row(CommunityEvent event) {
        try {
            Message encoded = converter.toMessage(event, new MessageProperties());
            return new ContentEventOutbox().setEventId(event.getEventId()).setDedupeKey(event.getDedupeKey())
                    .setEventType(event.getEventType().name()).setExchangeName(FixedEventRoutes.COMMUNITY_EXCHANGE)
                    .setRoutingKey(FixedEventRoutes.communityRoutingKey(event.getEventType()))
                    .setPayload(new String(encoded.getBody(), java.nio.charset.StandardCharsets.UTF_8))
                    .setStatus("SENDING").setAttempts(1);
        } catch (RuntimeException failure) {
            throw failure;
        }
    }

    private CommunityEvent event() {
        return new CommunityEvent().setSchemaVersion(1)
                .setEventId("550e8400-e29b-41d4-a716-446655440000")
                .setEventType(CommunityEventType.SOLUTION_LIKED)
                .setDedupeKey("solution-liked:2090000000000000000:100")
                .setOccurredAt("2026-10-03T10:00:00.123+08:00")
                .setActorId("100")
                .setSolutionId("2090000000000000000")
                .setRecipientIds(Collections.singletonList("101"));
    }
}
