package com.anishan.content.service;

import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.controller.CommunityEventAdminController;
import com.anishan.content.domain.entity.ContentEventOutbox;
import com.anishan.content.mapper.ContentEventOutboxMapper;
import com.anishan.content.service.impl.ContentEventOutboxServiceImpl;
import com.anishan.content.config.CommunityWorkerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityReplayTest {

    private static final String EVENT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 12, 0);

    private final ContentEventOutboxMapper mapper = mock(ContentEventOutboxMapper.class);
    private final ContentEventOutboxServiceImpl service = new ContentEventOutboxServiceImpl(
            mapper, new ObjectMapper(), new Jackson2JsonMessageConverter(), new CommunityWorkerProperties(),
            Clock.fixed(Instant.parse("2026-10-03T04:00:00Z"), ZoneId.of("Asia/Shanghai")));

    @Test
    void replaysOnlyFailedCommunityEventAndRetainsItsIdentity() {
        when(mapper.selectByEventIdForUpdate(EVENT_ID)).thenReturn(event("SOLUTION_LIKED", "FAILED"));
        when(mapper.replayCommunityFailed(EVENT_ID, NOW)).thenReturn(1);

        assertEquals("SOLUTION_LIKED", service.replayCommunityFailed(EVENT_ID));

        verify(mapper).selectByEventIdForUpdate(EVENT_ID);
        verify(mapper).replayCommunityFailed(EVENT_ID, NOW);
    }

    @Test
    void replayRejectsNonCommunityEventNonFailedAndMissingRows() {
        when(mapper.selectByEventIdForUpdate(EVENT_ID)).thenReturn(event("POINTS_AWARDED", "FAILED"));
        assertEquals(409, assertThrows(ApiStatusException.class,
                () -> service.replayCommunityFailed(EVENT_ID)).getStatusCode());
        verify(mapper, never()).replayCommunityFailed(EVENT_ID, NOW);

        when(mapper.selectByEventIdForUpdate(EVENT_ID)).thenReturn(event("SOLUTION_LIKED", "PENDING"));
        assertEquals(409, assertThrows(ApiStatusException.class,
                () -> service.replayCommunityFailed(EVENT_ID)).getStatusCode());

        when(mapper.selectByEventIdForUpdate(EVENT_ID)).thenReturn(null);
        assertEquals(404, assertThrows(ApiStatusException.class,
                () -> service.replayCommunityFailed(EVENT_ID)).getStatusCode());
        assertEquals(400, assertThrows(ApiStatusException.class,
                () -> service.replayCommunityFailed("not-an-event-id")).getStatusCode());
    }

    @Test
    void replayCasConflictReturnsConflictInsteadOfClaimingSuccess() {
        when(mapper.selectByEventIdForUpdate(EVENT_ID)).thenReturn(event("COMMENT_MODERATED", "FAILED"));
        when(mapper.replayCommunityFailed(EVENT_ID, NOW)).thenReturn(0);

        assertEquals(409, assertThrows(ApiStatusException.class,
                () -> service.replayCommunityFailed(EVENT_ID)).getStatusCode());
    }

    @Test
    void mapperReplaySqlIsStatusAndCommunityTypeFencedWithoutRewritingIdentity() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("mapper/ContentEventOutboxMapper.xml")) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, "ContentEventOutboxMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("eventId", EVENT_ID);
        params.put("now", NOW);

        String sql = configuration.getMappedStatement(
                "com.anishan.content.mapper.ContentEventOutboxMapper.replayCommunityFailed")
                .getBoundSql(params).getSql().toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
        assertTrue(sql.contains("event_type in"));
        assertTrue(sql.contains("status = 'failed'"));
        assertTrue(sql.contains("attempts = 0"));
        assertTrue(sql.contains("next_attempt_at = ?"));
        assertTrue(sql.contains("lease_owner = null"));
        assertTrue(sql.contains("lease_until = null"));
        assertTrue(sql.contains("last_error = null"));
        String assignments = sql.substring(0, sql.indexOf("where"));
        assertFalse(assignments.contains("event_id"));
        assertFalse(assignments.contains("dedupe_key"));
        assertFalse(assignments.contains("payload"));
    }

    @Test
    void adminEndpointRequiresSolutionEditPermission() throws Exception {
        PreAuthorize permission = CommunityEventAdminController.class
                .getMethod("replay", String.class).getAnnotation(PreAuthorize.class);
        assertNotNull(permission);
        assertEquals("hasAuthority('problem:solution:edit')", permission.value());
    }

    private static ContentEventOutbox event(String type, String status) {
        return new ContentEventOutbox().setEventId(EVENT_ID).setDedupeKey("solution-liked:1:2")
                .setEventType(type).setPayload("kept-payload").setStatus(status).setAttempts(20);
    }
}
