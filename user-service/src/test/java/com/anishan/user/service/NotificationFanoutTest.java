package com.anishan.user.service;

import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.user.config.NotificationWorkerProperties;
import com.anishan.user.domain.entity.NotificationFanoutJob;
import com.anishan.user.domain.enumeration.NotificationFanoutStatus;
import com.anishan.user.mapper.NotificationFanoutJobMapper;
import com.anishan.user.mapper.UserFollowMapper;
import com.anishan.user.mapper.UserNotificationMapper;
import com.anishan.user.service.impl.NotificationFanoutServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationFanoutTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 12, 0);
    private final NotificationFanoutJobMapper jobMapper = mock(NotificationFanoutJobMapper.class);
    private final UserFollowMapper followMapper = mock(UserFollowMapper.class);
    private final UserNotificationMapper notificationMapper = mock(UserNotificationMapper.class);
    private final NotificationWorkerProperties properties = new NotificationWorkerProperties();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CommunityEventFreshness freshness = new CommunityEventFreshness(
            Clock.fixed(Instant.parse("2026-10-03T04:00:00Z"), ZoneId.of("Asia/Shanghai")));
    private final NotificationFanoutServiceImpl service = new NotificationFanoutServiceImpl(
            jobMapper, followMapper, notificationMapper, properties, objectMapper, freshness,
            Clock.fixed(Instant.parse("2026-10-03T04:00:00Z"), ZoneId.of("Asia/Shanghai")));

    @Test
    void publishedDeliveryRegistersOneIdempotentJobWithoutWalkingFollowers() {
        CommunityEvent event = publishedEvent();
        when(jobMapper.insertIfAbsent(any())).thenReturn(1);
        when(jobMapper.selectByEventIdForUpdate(event.getEventId())).thenReturn(job(event, 0L));

        service.register(event);
        service.register(event);

        verify(jobMapper, times(2)).insertIfAbsent(any());
        verify(followMapper, never()).selectActiveFollowersForFanout(any(), any(), any(), anyInt());
        verify(notificationMapper, never()).insertNotificationRow(any());
    }

    @Test
    void fanoutProcessesTwoHundredThenOneAndAdvancesCursorInTheSameServiceTransaction()
            throws NoSuchMethodException {
        List<Long> firstBatch = LongStream.rangeClosed(1, 200).boxed().collect(Collectors.toList());
        NotificationFanoutJob first = job(publishedEvent(), 0L);
        makeClaimed(first);
        NotificationFanoutJob second = job(publishedEvent(), 200L);
        makeClaimed(second);
        when(jobMapper.selectByEventIdForUpdate(first.getEventId())).thenReturn(first, second);
        when(followMapper.selectActiveFollowersForFanout(7L, 0L, NOW, 200)).thenReturn(firstBatch);
        when(followMapper.selectActiveFollowersForFanout(7L, 200L, NOW, 200))
                .thenReturn(Collections.singletonList(201L));
        when(jobMapper.finishBatch(eq(first.getEventId()), eq("worker-a"), any(), anyString(), eq(NOW)))
                .thenReturn(1);
        doCallRealMethod().when(notificationMapper).insertNotification(any());
        when(notificationMapper.insertNotificationRow(any())).thenReturn(1);

        service.processOneBatch(first);
        service.processOneBatch(second);

        verify(followMapper).selectActiveFollowersForFanout(7L, 0L, NOW, 200);
        verify(followMapper).selectActiveFollowersForFanout(7L, 200L, NOW, 200);
        verify(jobMapper).finishBatch(first.getEventId(), "worker-a", 200L, "PENDING", NOW);
        verify(jobMapper).finishBatch(first.getEventId(), "worker-a", 201L, "DONE", NOW);
        verify(notificationMapper, times(201)).insertNotificationRow(any());
        assertTrue(NotificationFanoutServiceImpl.class.getMethod("processOneBatch", NotificationFanoutJob.class)
                .isAnnotationPresent(Transactional.class));
    }

    @Test
    void failedBatchDoesNotAdvanceCursorAndCanRetryFromTheSamePosition() {
        NotificationFanoutJob claimed = job(publishedEvent(), 0L);
        makeClaimed(claimed);
        when(jobMapper.selectByEventIdForUpdate(claimed.getEventId())).thenReturn(claimed);
        when(followMapper.selectActiveFollowersForFanout(7L, 0L, NOW, 200))
                .thenReturn(Collections.singletonList(1L));
        when(jobMapper.finishBatch(claimed.getEventId(), "worker-a", 1L, "DONE", NOW)).thenReturn(1);
        doCallRealMethod().when(notificationMapper).insertNotification(any());
        when(notificationMapper.insertNotificationRow(any()))
                .thenThrow(new IllegalStateException("simulated database failure"))
                .thenReturn(1);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> service.processOneBatch(claimed));
        verify(jobMapper, never()).finishBatch(anyString(), anyString(), any(), anyString(), any());

        service.processOneBatch(claimed);

        verify(followMapper, times(2)).selectActiveFollowersForFanout(7L, 0L, NOW, 200);
        verify(jobMapper).finishBatch(claimed.getEventId(), "worker-a", 1L, "DONE", NOW);
    }

    @Test
    void expiredCommunityJobCompletesWithoutSendingNotifications() {
        NotificationFanoutJob old = job(publishedEvent(), 0L);
        old.setOccurredAt(NOW.minusDays(181));
        makeClaimed(old);
        when(jobMapper.selectByEventIdForUpdate(old.getEventId())).thenReturn(old);
        when(jobMapper.finishBatch(old.getEventId(), "worker-a", 0L, "DONE", NOW)).thenReturn(1);

        service.processOneBatch(old);

        verify(followMapper, never()).selectActiveFollowersForFanout(any(), any(), any(), anyInt());
        verify(notificationMapper, never()).insertNotificationRow(any());
        verify(jobMapper).finishBatch(old.getEventId(), "worker-a", 0L, "DONE", NOW);
    }

    @Test
    void failedJobUsesExponentialBackoffAndTerminalFailureAtTwentyAttempts() {
        NotificationFanoutJob claimed = job(publishedEvent(), 0L);
        makeClaimed(claimed);
        claimed.setAttempts(2);
        when(jobMapper.markFailed(anyString(), anyString(), anyString(), any(LocalDateTime.class),
                anyString(), any(LocalDateTime.class))).thenReturn(1);

        assertTrue(service.markFailed(claimed, "FANOUT_FAILURE"));
        verify(jobMapper).markFailed(eq(claimed.getEventId()), eq("worker-a"), eq("PENDING"),
                eq(NOW.plusSeconds(10)), eq("FANOUT_FAILURE"), eq(NOW));

        claimed.setAttempts(20);
        assertTrue(service.markFailed(claimed, "FANOUT_FAILURE"));
        verify(jobMapper).markFailed(eq(claimed.getEventId()), eq("worker-a"), eq("FAILED"),
                eq(NOW), eq("FANOUT_FAILURE"), eq(NOW));
    }

    @Test
    void replayResetsOnlyFailedFanoutAndPreservesEventCursor() {
        String eventId = publishedEvent().getEventId();
        when(jobMapper.replayFailed(eventId, NOW)).thenReturn(1);

        service.replayFailed(eventId);

        verify(jobMapper).replayFailed(eventId, NOW);
        verify(jobMapper, never()).selectStatusByEventId(eventId);
    }

    @Test
    void replayRejectsMissingAndNonFailedFanoutJobs() {
        String eventId = publishedEvent().getEventId();
        when(jobMapper.replayFailed(eventId, NOW)).thenReturn(0);
        when(jobMapper.selectStatusByEventId(eventId)).thenReturn(null);
        assertEquals(404, assertThrows(com.anishan.commons.exception.ApiStatusException.class,
                () -> service.replayFailed(eventId)).getStatusCode());

        when(jobMapper.selectStatusByEventId(eventId)).thenReturn(NotificationFanoutStatus.DONE);
        assertEquals(409, assertThrows(com.anishan.commons.exception.ApiStatusException.class,
                () -> service.replayFailed(eventId)).getStatusCode());
        verify(jobMapper, times(2)).replayFailed(eventId, NOW);
    }

    @Test
    void mapperClaimsOnlyDueJobsAndFencesCursorUpdates() throws Exception {
        Configuration configuration = new Configuration();
        parse(configuration, "mapper/NotificationFanoutJobMapper.xml");
        parse(configuration, "mapper/UserFollowMapper.xml");
        parse(configuration, "mapper/UserNotificationMapper.xml");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("now", NOW);
        parameters.put("limit", 1);
        parameters.put("eventId", publishedEvent().getEventId());
        parameters.put("leaseOwner", "worker-a");
        parameters.put("leaseUntil", NOW.plusSeconds(60));
        parameters.put("leaseOwner", "worker-a");
        parameters.put("cursorUserId", 200L);
        parameters.put("status", "PENDING");
        parameters.put("nextAttemptAt", NOW.plusSeconds(5));
        parameters.put("errorCode", "FANOUT_FAILURE");
        parameters.put("followeeId", 7L);
        parameters.put("occurredAt", NOW);
        parameters.put("followerId", 100L);
        parameters.put("dedupeKey", "solution-published:2090000000000000001");
        parameters.put("recipientId", 100L);
        parameters.put("notificationId", 1L);
        parameters.put("pageSize", 20);
        parameters.put("offset", 0L);

        String claim = normalized(configuration.getMappedStatement(
                "com.anishan.user.mapper.NotificationFanoutJobMapper.selectClaimable")
                .getBoundSql(parameters).getSql());
        assertTrue(claim.contains("for update skip locked"));
        assertTrue(claim.contains("attempts < 20"));
        assertTrue(claim.contains("lease_until <= ?"));

        String finish = normalized(configuration.getMappedStatement(
                "com.anishan.user.mapper.NotificationFanoutJobMapper.finishBatch")
                .getBoundSql(parameters).getSql());
        assertTrue(finish.contains("lease_owner = ?"));
        assertTrue(finish.contains("lease_until > ?"));
        assertTrue(finish.contains("cursor_user_id = ?"));

        String followers = normalized(configuration.getMappedStatement(
                "com.anishan.user.mapper.UserFollowMapper.selectActiveFollowersForFanout")
                .getBoundSql(parameters).getSql());
        assertTrue(followers.contains("f.created_at <= ?"));
        assertTrue(followers.contains("u.status = 0"));
        assertTrue(followers.contains("f.follower_id > ?"));
        assertTrue(followers.contains("order by f.follower_id asc"));

        String duplicateCheck = normalized(configuration.getMappedStatement(
                "com.anishan.user.mapper.UserNotificationMapper.selectIdByRecipientAndDedupeForUpdate")
                .getBoundSql(parameters).getSql());
        assertTrue(duplicateCheck.endsWith("for update"));
        assertTrue(duplicateCheck.contains("recipient_id = ? and dedupe_key = ?"));
    }

    private NotificationFanoutJob job(CommunityEvent event, Long cursor) {
        NotificationFanoutJob job = new NotificationFanoutJob();
        job.setEventId(event.getEventId());
        job.setActorId(7L);
        job.setSolutionId(2090000000000000001L);
        job.setOccurredAt(NOW);
        job.setCursorUserId(cursor);
        job.setStatus(NotificationFanoutStatus.PENDING);
        job.setAttempts(0);
        job.setNextAttemptAt(NOW);
        job.setCreatedAt(NOW);
        job.setUpdatedAt(NOW);
        return job;
    }

    private void makeClaimed(NotificationFanoutJob job) {
        job.setStatus(NotificationFanoutStatus.SENDING);
        job.setLeaseOwner("worker-a");
        job.setLeaseUntil(NOW.plusSeconds(60));
        job.setAttempts(1);
    }

    private CommunityEvent publishedEvent() {
        return new CommunityEvent().setSchemaVersion(1)
                .setEventId("550e8400-e29b-41d4-a716-446655440000")
                .setEventType(CommunityEventType.SOLUTION_PUBLISHED)
                .setDedupeKey("solution-published:2090000000000000001")
                .setOccurredAt("2026-10-03T12:00:00+08:00")
                .setActorId("7").setSolutionId("2090000000000000001")
                .setRecipientIds(Collections.emptyList());
    }

    private static void parse(Configuration configuration, String path) throws Exception {
        try (InputStream input = NotificationFanoutTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, path);
            new XMLMapperBuilder(input, configuration, path, configuration.getSqlFragments()).parse();
        }
    }

    private static String normalized(String sql) {
        return sql.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
