package com.anishan.content.service;

import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.api.event.FixedEventRoutes;
import com.anishan.content.config.CommunityWorkerProperties;
import com.anishan.content.domain.entity.ContentEventOutbox;
import com.anishan.content.mapper.ContentEventOutboxMapper;
import com.anishan.content.service.impl.ContentEventOutboxServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.dao.DuplicateKeyException;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContentEventOutboxServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);
    private final ContentEventOutboxMapper mapper = mock(ContentEventOutboxMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MessageConverter converter = new Jackson2JsonMessageConverter();
    private final CommunityWorkerProperties properties = new CommunityWorkerProperties();
    private final ContentEventOutboxServiceImpl service = new ContentEventOutboxServiceImpl(
            mapper, objectMapper, converter, properties,
            Clock.fixed(Instant.parse("2026-10-03T02:00:00Z"), ZoneId.of("Asia/Shanghai")));

    @Test
    void recordCommunityUsesFixedRouteAndExactDuplicateIsIdempotent() {
        CommunityEvent event = event();
        when(mapper.selectByDedupeKey(event.getDedupeKey())).thenReturn(null);
        when(mapper.insert(any(ContentEventOutbox.class))).thenReturn(1);

        String firstId = service.recordCommunity(event);
        ContentEventOutbox inserted = captureInserted();
        when(mapper.selectByDedupeKey(event.getDedupeKey())).thenReturn(inserted);

        assertEquals(event.getEventId(), firstId);
        assertEquals(firstId, service.recordCommunity(event));
        assertEquals(CommunityEventType.SOLUTION_LIKED.name(), inserted.getEventType());
        assertEquals(FixedEventRoutes.COMMUNITY_EXCHANGE, inserted.getExchangeName());
        assertEquals("solution.liked.v1", inserted.getRoutingKey());
        assertEquals("PENDING", inserted.getStatus());
        assertEquals(0, inserted.getAttempts());
        verify(mapper, times(1)).insert(any(ContentEventOutbox.class));
    }

    @Test
    void sameDedupeKeyWithDifferentEventContentFailsWithoutLoggingPayload() {
        CommunityEvent event = event();
        ContentEventOutbox stored = pending(event);
        when(mapper.selectByDedupeKey(event.getDedupeKey())).thenReturn(stored);

        CommunityEvent differentEvent = event().setEventId("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> service.recordCommunity(differentEvent));

        assertEquals("Content outbox dedupe key has different content", failure.getMessage());
        assertFalse(failure.getMessage().contains(event.getSolutionId()));
        verify(mapper, never()).insert(any(ContentEventOutbox.class));
    }

    @Test
    void concurrentDedupeInsertReturnsTheWinnerOnlyWhenThePayloadMatches() {
        CommunityEvent event = event();
        ContentEventOutbox winner = pending(event);
        when(mapper.selectByDedupeKey(event.getDedupeKey())).thenReturn(null);
        when(mapper.insert(any(ContentEventOutbox.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(mapper.selectByDedupeKeyForUpdate(event.getDedupeKey())).thenReturn(winner);

        assertEquals(event.getEventId(), service.recordCommunity(event));
        verify(mapper).selectByDedupeKeyForUpdate(event.getDedupeKey());
    }

    @Test
    void invalidRecipientsAreRejectedBeforeAnyDatabaseWrite() {
        CommunityEvent invalid = event().setRecipientIds(Collections.singletonList("100"));

        assertThrows(IllegalArgumentException.class, () -> service.recordCommunity(invalid));

        verifyNoInteractions(mapper);
    }

    @Test
    void expiredClaimIsReclaimedAndUsesOwnerBoundStateTransitions() {
        ContentEventOutbox due = pending(event()).setAttempts(1).setStatus("SENDING")
                .setLeaseUntil(NOW.minusSeconds(1));
        when(mapper.selectClaimable(NOW, 20)).thenReturn(Collections.singletonList(due));
        when(mapper.markClaimed(eq(due.getEventId()), eq("worker-a"), eq(NOW), eq(NOW.plusSeconds(60))))
                .thenReturn(1);

        assertEquals(1, service.claimBatch("worker-a", 20).size());

        verify(mapper).failExhaustedExpiredClaims(NOW, 20);
        verify(mapper).markClaimed(due.getEventId(), "worker-a", NOW, NOW.plusSeconds(60));
        verify(mapper, never()).markSent(eq(due.getEventId()), eq("other-worker"), any(LocalDateTime.class));
    }

    @Test
    void failedEventsBackOffAndBecomeTerminalAtTwentyAttempts() {
        ContentEventOutbox claimed = pending(event()).setAttempts(3).setStatus("SENDING");
        when(mapper.markFailed(anyString(), anyString(), anyString(), any(LocalDateTime.class),
                anyString(), any(LocalDateTime.class))).thenReturn(1);

        assertTrue(service.markFailed(claimed, "worker-a", "BROKER_NACK"));
        verify(mapper).markFailed(eq(claimed.getEventId()), eq("worker-a"), eq("PENDING"),
                eq(NOW.plusSeconds(20)), eq("BROKER_NACK"), eq(NOW));

        claimed.setAttempts(20);
        assertTrue(service.markFailed(claimed, "worker-a", "BROKER_NACK"));
        verify(mapper).markFailed(eq(claimed.getEventId()), eq("worker-a"), eq("FAILED"),
                eq(NOW), eq("BROKER_NACK"), eq(NOW));
    }

    @Test
    void cleanupUsesThirtyDayRetentionAndHardFiveHundredRowCap() {
        properties.setCleanupBatchSize(500);
        when(mapper.deleteExpiredSent(any(LocalDateTime.class), anyInt())).thenReturn(500);

        assertEquals(500, service.cleanupSentBatch());

        verify(mapper).deleteExpiredSent(NOW.minusDays(30), 500);
    }

    @Test
    void mapperSqlHasTypeAllowListSkipLockedFencingAndSentOnlyRetention() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getResourceAsStream("/mapper/ContentEventOutboxMapper.xml")) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, "ContentEventOutboxMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("now", NOW);
        parameters.put("limit", 20);
        parameters.put("eventId", event().getEventId());
        parameters.put("leaseOwner", "worker-a");
        parameters.put("leaseUntil", NOW.plusSeconds(60));
        parameters.put("status", "PENDING");
        parameters.put("nextAttemptAt", NOW.plusSeconds(5));
        parameters.put("errorCode", "BROKER_NACK");
        parameters.put("sentBefore", NOW.minusDays(30));
        parameters.put("dedupeKey", event().getDedupeKey());

        String claim = normalized(configuration.getMappedStatement(
                "com.anishan.content.mapper.ContentEventOutboxMapper.selectClaimable")
                .getBoundSql(parameters).getSql());
        assertTrue(claim.contains("for update skip locked"));
        assertTrue(claim.contains("event_type in"));
        assertTrue(claim.contains("attempts < 20"));
        assertTrue(claim.contains("lease_until <= ?"));
        assertFalse(claim.contains("points_awarded"));

        String sent = normalized(configuration.getMappedStatement(
                "com.anishan.content.mapper.ContentEventOutboxMapper.markSent")
                .getBoundSql(parameters).getSql());
        assertTrue(sent.contains("lease_owner = ?"));
        assertTrue(sent.contains("lease_until > ?"));

        String failed = normalized(configuration.getMappedStatement(
                "com.anishan.content.mapper.ContentEventOutboxMapper.markFailed")
                .getBoundSql(parameters).getSql());
        assertTrue(failed.contains("lease_owner = ?"));
        assertTrue(failed.contains("lease_until > ?"));

        String cleanup = normalized(configuration.getMappedStatement(
                "com.anishan.content.mapper.ContentEventOutboxMapper.deleteExpiredSent")
                .getBoundSql(parameters).getSql());
        assertTrue(cleanup.contains("status = 'sent'"));
        assertTrue(cleanup.contains("sent_at < ?"));
        assertTrue(cleanup.contains("limit ?"));
        assertFalse(cleanup.contains("failed"));
    }

    private ContentEventOutbox captureInserted() {
        org.mockito.ArgumentCaptor<ContentEventOutbox> captor =
                org.mockito.ArgumentCaptor.forClass(ContentEventOutbox.class);
        verify(mapper).insert(captor.capture());
        return captor.getValue();
    }

    private ContentEventOutbox pending(CommunityEvent event) {
        Message encoded = converter.toMessage(event, new MessageProperties());
        return new ContentEventOutbox().setEventId(event.getEventId()).setDedupeKey(event.getDedupeKey())
                .setEventType(event.getEventType().name()).setExchangeName(FixedEventRoutes.COMMUNITY_EXCHANGE)
                .setRoutingKey(FixedEventRoutes.communityRoutingKey(event.getEventType()))
                .setPayload(new String(encoded.getBody(), java.nio.charset.StandardCharsets.UTF_8))
                .setStatus("PENDING").setAttempts(0).setNextAttemptAt(NOW).setCreatedAt(NOW);
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

    private static String normalized(String sql) {
        return sql.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
