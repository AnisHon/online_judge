package com.anishan.problem.service;

import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.event.PointAwardEvent;
import com.anishan.problem.config.ProblemOutboxProperties;
import com.anishan.problem.domain.entity.ProblemEventOutbox;
import com.anishan.problem.mapper.ProblemEventOutboxMapper;
import com.anishan.problem.service.impl.ProblemEventOutboxServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.springframework.dao.DuplicateKeyException;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import java.io.InputStream;
import java.math.BigDecimal;
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

class ProblemEventOutboxServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);
    private final ProblemEventOutboxMapper mapper = mock(ProblemEventOutboxMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MessageConverter converter = converter();
    private final ProblemOutboxProperties properties = new ProblemOutboxProperties();
    private final ProblemEventOutboxServiceImpl service = new ProblemEventOutboxServiceImpl(
            mapper, objectMapper, converter, properties,
            Clock.fixed(Instant.parse("2026-10-03T02:00:00Z"), ZoneId.of("Asia/Shanghai")));

    @Test
    void pointAwardUsesFixedRouteAndDuplicateDedupeIsIdempotent() throws Exception {
        PointAwardEvent event = points("2.00");
        when(mapper.selectByDedupeKey(event.getDedupeKey())).thenReturn(null);
        when(mapper.insert(any(ProblemEventOutbox.class))).thenReturn(1);
        String firstId = service.recordPointAward(event);
        ProblemEventOutbox inserted = captureInserted();
        when(mapper.selectByDedupeKey(event.getDedupeKey())).thenReturn(inserted);

        String duplicateId = service.recordPointAward(event);

        assertEquals(event.getEventId(), firstId);
        assertEquals(firstId, duplicateId);
        assertEquals("POINTS_AWARDED", inserted.getEventType());
        assertEquals("account.points.v1", inserted.getExchangeName());
        assertEquals("points.awarded.v1", inserted.getRoutingKey());
        verify(mapper, times(1)).insert(any(ProblemEventOutbox.class));
    }

    @Test
    void sameDedupeWithDifferentPayloadFailsWithoutExposingPayload() {
        PointAwardEvent original = points("2.00");
        ProblemEventOutbox stored = pendingPoints(original);
        when(mapper.selectByDedupeKey(original.getDedupeKey())).thenReturn(stored);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> service.recordPointAward(points("3.00")));

        assertEquals("Problem outbox dedupe key has different content", failure.getMessage());
        verify(mapper, never()).insert(any(ProblemEventOutbox.class));
    }

    @Test
    void concurrentDedupeInsertReturnsTheWinnerWhenPayloadMatches() throws Exception {
        PointAwardEvent event = points("2.00");
        ProblemEventOutbox winner = pendingPoints(event);
        when(mapper.selectByDedupeKey(event.getDedupeKey())).thenReturn(null);
        when(mapper.selectByDedupeKeyForUpdate(event.getDedupeKey())).thenReturn(winner);
        when(mapper.insert(any(ProblemEventOutbox.class))).thenThrow(new DuplicateKeyException("duplicate"));

        assertEquals(event.getEventId(), service.recordPointAward(event));

        verify(mapper).selectByDedupeKey(event.getDedupeKey());
        verify(mapper).selectByDedupeKeyForUpdate(event.getDedupeKey());
    }

    @Test
    void judgePayloadAcceptsExactSixHundredKiBAndRejectsOneByteOver() throws Exception {
        JudgeInfo boundary = judgeInfo("");
        int emptyBodySize = converter.toMessage(boundary, new MessageProperties()).getBody().length;
        boundary.setCode(repeat('x', ProblemEventOutboxService.MAX_JUDGE_PAYLOAD_BYTES - emptyBodySize));
        assertEquals(ProblemEventOutboxService.MAX_JUDGE_PAYLOAD_BYTES,
                converter.toMessage(boundary, new MessageProperties()).getBody().length);
        when(mapper.selectByDedupeKey(anyString())).thenReturn(null);
        when(mapper.insert(any(ProblemEventOutbox.class))).thenReturn(1);

        assertDoesNotThrow(() -> service.recordJudgeDispatch(boundary));

        JudgeInfo tooLarge = judgeInfo(boundary.getCode() + "x");
        assertThrows(IllegalArgumentException.class, () -> service.recordJudgeDispatch(tooLarge));
        verify(mapper, times(1)).insert(any(ProblemEventOutbox.class));
    }

    @Test
    void expiredLeaseRecoveryAndClaimRemainRestrictedToProblemOwnedTypes() {
        ProblemEventOutbox community = new ProblemEventOutbox().setEventId("community-event")
                .setEventType("SOLUTION_PUBLISHED").setAttempts(1);
        when(mapper.selectClaimable(any(LocalDateTime.class), eq(20))).thenReturn(Collections.singletonList(community));

        assertTrue(service.claimBatch("relay-1", 20).isEmpty());

        verify(mapper).failExhaustedExpiredClaims(NOW, 20);
        verify(mapper).selectClaimable(NOW, 20);
        verify(mapper, never()).markClaimed(anyString(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void failedAttemptUsesExponentialBackoffAndTwentyAttemptsBecomeTerminal() {
        ProblemEventOutbox claimed = new ProblemEventOutbox().setEventId(UUID.randomUUID().toString())
                .setEventType(ProblemEventOutboxService.JUDGE_DISPATCH).setAttempts(3);
        when(mapper.markFailed(anyString(), anyString(), anyString(), any(LocalDateTime.class), anyString(), any(LocalDateTime.class)))
                .thenReturn(1);

        assertTrue(service.markFailed(claimed, "relay-1", "BROKER_NACK"));

        verify(mapper).markFailed(eq(claimed.getEventId()), eq("relay-1"), eq("PENDING"),
                eq(NOW.plusSeconds(20)), eq("BROKER_NACK"), eq(NOW));
        claimed.setAttempts(20);
        service.markFailed(claimed, "relay-1", "BROKER_NACK");
        verify(mapper).markFailed(eq(claimed.getEventId()), eq("relay-1"), eq("FAILED"),
                eq(NOW), eq("BROKER_NACK"), eq(NOW));
    }

    @Test
    void cleanupUsesPerTypeRetentionAndHardFiveHundredRowCap() {
        when(mapper.deleteExpiredSent(any(LocalDateTime.class), any(LocalDateTime.class), anyInt())).thenReturn(500);

        assertEquals(500, service.cleanupSentBatch());

        verify(mapper).deleteExpiredSent(NOW.minusDays(30), NOW.minusHours(24), 500);
    }

    @Test
    void mapperSqlParsesAndEnforcesAllowlistFencingAndRetentionFilters() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getResourceAsStream("/mapper/ProblemEventOutboxMapper.xml")) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, "ProblemEventOutboxMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("now", NOW);
        parameters.put("limit", 20);
        parameters.put("eventId", "e");
        parameters.put("leaseOwner", "owner");
        parameters.put("leaseUntil", NOW.plusSeconds(60));
        parameters.put("status", "PENDING");
        parameters.put("nextAttemptAt", NOW.plusSeconds(5));
        parameters.put("errorCode", "BROKER_NACK");
        parameters.put("pointsBefore", NOW.minusDays(30));
        parameters.put("judgeBefore", NOW.minusHours(24));
        parameters.put("dedupeKey", "dedupe");

        String duplicateRead = normalized(configuration.getMappedStatement(
                "com.anishan.problem.mapper.ProblemEventOutboxMapper.selectByDedupeKeyForUpdate")
                .getBoundSql(parameters).getSql());
        assertTrue(duplicateRead.endsWith("for update"));

        String claim = normalized(configuration.getMappedStatement(
                "com.anishan.problem.mapper.ProblemEventOutboxMapper.selectClaimable").getBoundSql(parameters).getSql());
        assertTrue(claim.contains("event_type in ('points_awarded', 'judge_dispatch')"));
        assertTrue(claim.contains("for update skip locked"));
        assertTrue(claim.contains("attempts < 20"));

        String sent = normalized(configuration.getMappedStatement(
                "com.anishan.problem.mapper.ProblemEventOutboxMapper.markSent").getBoundSql(parameters).getSql());
        assertTrue(sent.contains("lease_owner = ?"));
        assertTrue(sent.contains("lease_until > ?"));
        assertTrue(sent.contains("event_type in ('points_awarded', 'judge_dispatch')"));

        String failed = normalized(configuration.getMappedStatement(
                "com.anishan.problem.mapper.ProblemEventOutboxMapper.markFailed").getBoundSql(parameters).getSql());
        assertTrue(failed.contains("lease_owner = ?"));
        assertTrue(failed.contains("lease_until > ?"));

        String unsupported = normalized(configuration.getMappedStatement(
                "com.anishan.problem.mapper.ProblemEventOutboxMapper.selectUnsupportedDue")
                .getBoundSql(parameters).getSql());
        // Historical pre-cutover community rows stay quarantined, never dispatched by problem.
        assertTrue(unsupported.contains("solution_published"));
        assertFalse(unsupported.contains("payload"));
        assertFalse(unsupported.contains("attempts < 20"));

        String cleanup = normalized(configuration.getMappedStatement(
                "com.anishan.problem.mapper.ProblemEventOutboxMapper.deleteExpiredSent").getBoundSql(parameters).getSql());
        assertTrue(cleanup.contains("status = 'sent'"));
        assertTrue(cleanup.contains("event_type = 'points_awarded'"));
        assertTrue(cleanup.contains("event_type = 'judge_dispatch'"));
        assertFalse(cleanup.contains("solution_published"));
        assertTrue(cleanup.contains("limit ?"));
    }

    private ProblemEventOutbox captureInserted() {
        org.mockito.ArgumentCaptor<ProblemEventOutbox> captor = org.mockito.ArgumentCaptor.forClass(ProblemEventOutbox.class);
        verify(mapper).insert(captor.capture());
        return captor.getValue();
    }

    private PointAwardEvent points(String amount) {
        return new PointAwardEvent().setSchemaVersion(1).setEventId("b3a7a3c0-dc2b-4ac7-8d22-a4dd96a18712")
                .setDedupeKey("points-awarded:7:11").setOccurredAt("2026-10-03T10:00:00+08:00")
                .setUserId("7").setProblemId("11").setAmount(amount);
    }

    private ProblemEventOutbox pendingPoints(PointAwardEvent event) {
        Message encoded = converter.toMessage(event, new MessageProperties());
        return new ProblemEventOutbox().setEventId(event.getEventId()).setDedupeKey(event.getDedupeKey())
                .setEventType(ProblemEventOutboxService.POINTS_AWARDED).setExchangeName("account.points.v1")
                .setRoutingKey("points.awarded.v1").setPayload(new String(encoded.getBody(), java.nio.charset.StandardCharsets.UTF_8));
    }

    private JudgeInfo judgeInfo(String code) {
        return new JudgeInfo().setSubmitId(101L).setUserId(7L).setProblemId(11L).setLanguageId(3L)
                .setLanguage("java").setCode(code).setListScore(new BigDecimal("100"));
    }

    private static MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    private static String repeat(char value, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, value);
        return new String(chars);
    }

    private static String normalized(String sql) {
        return sql.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
