package com.anishan.problem.job;

import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.event.PointAwardEvent;
import com.anishan.problem.config.ProblemMessagingConfig;
import com.anishan.problem.config.ProblemOutboxProperties;
import com.anishan.problem.domain.entity.ProblemEventOutbox;
import com.anishan.problem.service.ProblemEventOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProblemOutboxRelayTest {

    @Test
    void deadLetterTopologyAddsOnlyTheNewJudgeDlqAndDoesNotRedeclareLegacyQueue() {
        Declarables topology = new ProblemMessagingConfig().judgeDispatchDeadLetterTopology();

        assertEquals(1, topology.getDeclarablesByType(DirectExchange.class).size());
        assertEquals(ProblemMessagingConfig.JUDGE_DEAD_LETTER_EXCHANGE,
                topology.getDeclarablesByType(DirectExchange.class).get(0).getName());
        assertEquals(1, topology.getDeclarablesByType(Queue.class).size());
        assertEquals(ProblemMessagingConfig.JUDGE_DEAD_LETTER_QUEUE,
                topology.getDeclarablesByType(Queue.class).get(0).getName());
        assertEquals(1, topology.getDeclarablesByType(Binding.class).size());
        Binding binding = topology.getDeclarablesByType(Binding.class).get(0);
        assertEquals(ProblemMessagingConfig.JUDGE_DEAD_LETTER_QUEUE, binding.getDestination());
        assertEquals(ProblemMessagingConfig.JUDGE_DEAD_LETTER_EXCHANGE, binding.getExchange());
        assertEquals(ProblemMessagingConfig.JUDGE_DEAD_LETTER_ROUTING_KEY, binding.getRoutingKey());
        assertFalse(topology.getDeclarablesByType(Queue.class).stream()
                .anyMatch(queue -> "judge-info-queue".equals(queue.getName())));
    }

    private final ProblemEventOutboxService service = mock(ProblemEventOutboxService.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final MessageConverter converter = new Jackson2JsonMessageConverter();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProblemOutboxProperties properties = new ProblemOutboxProperties();
    private ThreadPoolTaskExecutor executor;
    private ProblemOutboxRelay relay;

    @BeforeEach
    void setUp() {
        properties.setConfirmTimeoutSeconds(1);
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.initialize();
        relay = new ProblemOutboxRelay(service, rabbitTemplate, converter, objectMapper, properties, executor,
                Clock.fixed(Instant.parse("2026-10-03T02:00:00Z"), ZoneId.of("Asia/Shanghai")));
        when(service.findUnsupportedDueEvents(20)).thenReturn(Collections.emptyList());
        when(service.markSent(anyString(), anyString())).thenReturn(true);
        when(service.markFailed(any(ProblemEventOutbox.class), anyString(), anyString())).thenReturn(true);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdown();
        executor.getThreadPoolExecutor().awaitTermination(3, TimeUnit.SECONDS);
    }

    @Test
    void ackedJudgeDispatchUsesExistingWireShapePersistentDeliveryAndEventMessageId() throws Exception {
        JudgeInfo judgeInfo = judgeInfo();
        ProblemEventOutbox row = judgeEvent(judgeInfo);
        when(service.claimBatch(anyString(), eq(20))).thenReturn(Collections.singletonList(row));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().set(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(eq(ProblemMessagingConfig.JUDGE_EXCHANGE),
                eq(ProblemMessagingConfig.JUDGE_ROUTING_KEY), any(Message.class), any(CorrelationData.class));

        relay.poll();

        org.mockito.ArgumentCaptor<Message> messageCaptor = org.mockito.ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate, timeout(2000)).send(eq(ProblemMessagingConfig.JUDGE_EXCHANGE),
                eq(ProblemMessagingConfig.JUDGE_ROUTING_KEY), messageCaptor.capture(), any(CorrelationData.class));
        Message sent = messageCaptor.getValue();
        assertEquals(row.getEventId(), sent.getMessageProperties().getMessageId());
        assertEquals(MessageDeliveryMode.PERSISTENT, sent.getMessageProperties().getDeliveryMode());
        assertEquals(judgeInfo, objectMapper.readValue(sent.getBody(), JudgeInfo.class));
        verify(service, timeout(2000)).markSent(eq(row.getEventId()), anyString());
        verify(service, never()).markFailed(any(ProblemEventOutbox.class), anyString(), anyString());
    }

    @Test
    void brokerNackIsRetriedAndNotMarkedSent() {
        ProblemEventOutbox row = judgeEvent(judgeInfo());
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
        ProblemEventOutbox row = judgeEvent(judgeInfo());
        when(service.claimBatch(anyString(), eq(20))).thenReturn(Collections.singletonList(row));
        doAnswer(invocation -> {
            Message message = invocation.getArgument(2);
            CorrelationData correlation = invocation.getArgument(3);
            correlation.setReturned(new ReturnedMessage(message, 312, "NO_ROUTE",
                    ProblemMessagingConfig.JUDGE_EXCHANGE, ProblemMessagingConfig.JUDGE_ROUTING_KEY));
            correlation.getFuture().set(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        relay.poll();

        verify(service, timeout(2000)).markFailed(eq(row), anyString(), eq("MESSAGE_RETURNED"));
        verify(service, never()).markSent(anyString(), anyString());
    }

    @Test
    void confirmTimeoutMovesEventToRetryWithoutInventingAResult() {
        ProblemEventOutbox row = judgeEvent(judgeInfo());
        when(service.claimBatch(anyString(), eq(20))).thenReturn(Collections.singletonList(row));
        doNothing().when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        relay.poll();

        verify(service, timeout(2500)).markFailed(eq(row), anyString(), eq("CONFIRM_TIMEOUT"));
        verify(service, never()).markSent(anyString(), anyString());
    }

    @Test
    void databaseFailureAfterBrokerAckDoesNotMislabelConfirmedPublishAsFailed() {
        ProblemEventOutbox row = judgeEvent(judgeInfo());
        when(service.claimBatch(anyString(), eq(20))).thenReturn(Collections.singletonList(row));
        when(service.markSent(eq(row.getEventId()), anyString())).thenThrow(new IllegalStateException("database"));
        doAnswer(invocation -> {
            ((CorrelationData) invocation.getArgument(3)).getFuture()
                    .set(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        relay.poll();

        verify(service, timeout(2000)).markSent(eq(row.getEventId()), anyString());
        verify(service, never()).markFailed(any(ProblemEventOutbox.class), anyString(), anyString());
    }

    @Test
    void wrongTypeOrRouteIsNeverPublished() {
        ProblemEventOutbox row = judgeEvent(judgeInfo()).setExchangeName("caller-controlled-exchange");
        when(service.claimBatch(anyString(), eq(20))).thenReturn(Collections.singletonList(row));

        relay.poll();

        verify(service, timeout(2000)).markFailed(eq(row), anyString(), eq("INVALID_PAYLOAD"));
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void pointsBranchUsesTheFixedPointsRouteAndEventId() throws Exception {
        PointAwardEvent event = new PointAwardEvent().setSchemaVersion(1)
                .setEventId("f9d30c30-21d8-4e83-a25c-c4e7c1f90c34")
                .setDedupeKey("points-awarded:7:11").setOccurredAt("2026-10-03T10:00:00+08:00")
                .setUserId("7").setProblemId("11").setAmount("2.00");
        Message encoded = converter.toMessage(event, new MessageProperties());
        ProblemEventOutbox row = new ProblemEventOutbox().setEventId(event.getEventId())
                .setDedupeKey(event.getDedupeKey()).setEventType(ProblemEventOutboxService.POINTS_AWARDED)
                .setExchangeName("account.points.v1").setRoutingKey("points.awarded.v1")
                .setPayload(new String(encoded.getBody(), java.nio.charset.StandardCharsets.UTF_8));
        when(service.claimBatch(anyString(), eq(20))).thenReturn(Collections.singletonList(row));
        doAnswer(invocation -> {
            ((CorrelationData) invocation.getArgument(3)).getFuture()
                    .set(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        relay.poll();

        org.mockito.ArgumentCaptor<Message> messageCaptor = org.mockito.ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate, timeout(2000)).send(eq("account.points.v1"), eq("points.awarded.v1"),
                messageCaptor.capture(), any(CorrelationData.class));
        assertEquals(event.getEventId(), messageCaptor.getValue().getMessageProperties().getMessageId());
        PointAwardEvent decoded = objectMapper.readValue(messageCaptor.getValue().getBody(), PointAwardEvent.class);
        assertEquals(event.getEventId(), decoded.getEventId());
        assertEquals(event.getDedupeKey(), decoded.getDedupeKey());
        assertEquals(event.getAmount(), decoded.getAmount());
        verify(service, timeout(2000)).markSent(eq(event.getEventId()), anyString());
    }

    private ProblemEventOutbox judgeEvent(JudgeInfo judgeInfo) {
        String eventId = UUID.randomUUID().toString();
        try {
            return new ProblemEventOutbox().setEventId(eventId)
                    .setDedupeKey("judge-dispatch:" + judgeInfo.getSubmitId())
                    .setEventType(ProblemEventOutboxService.JUDGE_DISPATCH)
                    .setExchangeName(ProblemMessagingConfig.JUDGE_EXCHANGE)
                    .setRoutingKey(ProblemMessagingConfig.JUDGE_ROUTING_KEY)
                    .setPayload(objectMapper.writeValueAsString(judgeInfo));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private JudgeInfo judgeInfo() {
        return new JudgeInfo().setSubmitId(91L).setUserId(7L).setProblemId(11L).setLanguageId(3L)
                .setLanguage("java").setCode("public class Main {}")
                .setListScore(new BigDecimal("100"));
    }
}
