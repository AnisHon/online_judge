package com.anishan.user.service;

import com.anishan.api.client.user.domain.vo.UserSummaryVo;
import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.user.controller.NotificationController;
import com.anishan.user.domain.dto.NotificationPageQuery;
import com.anishan.user.domain.entity.UserNotification;
import com.anishan.user.domain.vo.NotificationVo;
import com.anishan.user.mapper.NotificationFanoutJobMapper;
import com.anishan.user.mapper.UserNotificationMapper;
import com.anishan.user.service.impl.NotificationQueryServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationQueryTest {

    private static final long RECIPIENT_ID = 77L;
    private static final long LARGE_ID = 2098765432109876543L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 12, 0);

    private final UserNotificationMapper notificationMapper = mock(UserNotificationMapper.class);
    private final NotificationFanoutJobMapper fanoutMapper = mock(NotificationFanoutJobMapper.class);
    private final SysUserService userService = mock(SysUserService.class);
    private final NotificationQueryServiceImpl service = new NotificationQueryServiceImpl(
            notificationMapper, fanoutMapper, userService,
            Clock.fixed(Instant.parse("2026-10-03T04:00:00Z"), ZoneId.of("Asia/Shanghai")));

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void pageLimitsSizeBatchesActorSummariesAndHidesUntrustedModerationFields() {
        NotificationPageQuery invalid = query(51);
        assertEquals(400, assertThrows(ApiStatusException.class,
                () -> service.page(RECIPIENT_ID, invalid)).getStatusCode());
        verifyNoInteractions(notificationMapper, userService);

        UserNotification unknown = notification(LARGE_ID, 901L, "UNRECOGNIZED", "DELETE", "internal detail");
        UserNotification moderated = notification(LARGE_ID - 1, 902L, "COMMENT_MODERATED", "DELETE", "policy reason");
        when(notificationMapper.selectPageByRecipient(RECIPIENT_ID, false, 0L, 20))
                .thenReturn(Arrays.asList(unknown, moderated));
        when(notificationMapper.countByRecipient(RECIPIENT_ID, false)).thenReturn(2L);
        UserSummaryVo actor = new UserSummaryVo();
        actor.setUserId(902L);
        when(userService.getUserSummariesByIds(Arrays.asList(901L, 902L)))
                .thenReturn(Collections.singletonList(actor));

        List<NotificationVo> rows = service.page(RECIPIENT_ID, query(20)).getData();

        assertEquals(2, rows.size());
        assertEquals("UNKNOWN", rows.get(0).getType());
        assertNull(rows.get(0).getAction());
        assertNull(rows.get(0).getReason());
        assertEquals("DELETE", rows.get(1).getAction());
        assertEquals("policy reason", rows.get(1).getReason());
        assertEquals(actor, rows.get(1).getActor());
        verify(userService).getUserSummariesByIds(Arrays.asList(901L, 902L));
        verify(notificationMapper).countByRecipient(RECIPIENT_ID, false);
    }

    @Test
    void emptyPageIsLegalAndUsesRecipientScopedCount() {
        when(notificationMapper.selectPageByRecipient(RECIPIENT_ID, true, 20L, 20))
                .thenReturn(Collections.emptyList());
        when(notificationMapper.countByRecipient(RECIPIENT_ID, true)).thenReturn(0L);

        assertTrue(service.page(RECIPIENT_ID, query(20, 2L, true)).getData().isEmpty());
        verify(notificationMapper).countByRecipient(RECIPIENT_ID, true);
        verifyNoInteractions(userService);
    }

    @Test
    void readIsRecipientScopedAndIdempotentButUnknownOrOtherUsersRowsAreNotFound() {
        when(notificationMapper.markRead(LARGE_ID, RECIPIENT_ID, NOW)).thenReturn(0);
        when(notificationMapper.existsByRecipientAndId(RECIPIENT_ID, LARGE_ID)).thenReturn(1);
        service.markRead(RECIPIENT_ID, LARGE_ID);

        verify(notificationMapper).markRead(LARGE_ID, RECIPIENT_ID, NOW);
        verify(notificationMapper).existsByRecipientAndId(RECIPIENT_ID, LARGE_ID);

        when(notificationMapper.markRead(LARGE_ID + 1, RECIPIENT_ID, NOW)).thenReturn(0);
        when(notificationMapper.existsByRecipientAndId(RECIPIENT_ID, LARGE_ID + 1)).thenReturn(0);
        assertEquals(404, assertThrows(ApiStatusException.class,
                () -> service.markRead(RECIPIENT_ID, LARGE_ID + 1)).getStatusCode());
    }

    @Test
    void readAllUsesBoundedThroughIdAndRejectsOutOfRangeLong() {
        when(notificationMapper.markAllRead(RECIPIENT_ID, LARGE_ID, NOW)).thenReturn(3);
        assertEquals(3, service.markAllRead(RECIPIENT_ID, String.valueOf(LARGE_ID)));
        verify(notificationMapper).markAllRead(RECIPIENT_ID, LARGE_ID, NOW);

        assertEquals(400, assertThrows(ApiStatusException.class, () -> service.markAllRead(
                RECIPIENT_ID, "9223372036854775808")).getStatusCode());
    }

    @Test
    void controllerDerivesRecipientFromAuthenticatedPrincipalAndAdminReplayIsPermissionGated() {
        LoginUser principal = new LoginUser();
        principal.setUser(new SysUser().setUserId(RECIPIENT_ID));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "ignored", Collections.emptyList()));
        NotificationController controller = new NotificationController(service, mock(NotificationFanoutService.class));
        when(notificationMapper.selectPageByRecipient(RECIPIENT_ID, false, 0L, 20))
                .thenReturn(Collections.emptyList());
        when(notificationMapper.countByRecipient(RECIPIENT_ID, false)).thenReturn(0L);

        controller.page(query(20));

        verify(notificationMapper).selectPageByRecipient(RECIPIENT_ID, false, 0L, 20);
        try {
            PreAuthorize permission = NotificationController.class
                    .getMethod("replayFanout", String.class).getAnnotation(PreAuthorize.class);
            assertNotNull(permission);
            assertEquals("hasAuthority('user:user:edit')", permission.value());
        } catch (NoSuchMethodException missing) {
            throw new AssertionError(missing);
        }
    }

    @Test
    void browserIdsAreSerializedAsStringsAndMapperAlwaysScopesByRecipient() throws Exception {
        NotificationVo vo = new NotificationVo();
        vo.setNotificationId(LARGE_ID);
        vo.setSolutionId(LARGE_ID - 1);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(vo));
        assertTrue(json.get("notificationId").isTextual());
        assertEquals(String.valueOf(LARGE_ID), json.get("notificationId").asText());
        assertTrue(json.get("solutionId").isTextual());

        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("mapper/UserNotificationMapper.xml")) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, "UserNotificationMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("recipientId", RECIPIENT_ID);
        params.put("notificationId", LARGE_ID);
        params.put("unreadOnly", true);
        params.put("offset", 0L);
        params.put("pageSize", 20);
        params.put("throughId", LARGE_ID);
        params.put("readAt", NOW);
        params.put("createdBefore", NOW.minusDays(180));
        params.put("completedBefore", NOW.minusDays(30));
        params.put("limit", 500);

        String pageSql = normalize(configuration.getMappedStatement(
                "com.anishan.user.mapper.UserNotificationMapper.selectPageByRecipient")
                .getBoundSql(params).getSql());
        assertTrue(pageSql.contains("recipient_id = ?"));
        assertTrue(pageSql.contains("read_at is null"));
        assertTrue(pageSql.contains("order by notification_id desc"));

        String readAllSql = normalize(configuration.getMappedStatement(
                "com.anishan.user.mapper.UserNotificationMapper.markAllRead")
                .getBoundSql(params).getSql());
        assertTrue(readAllSql.contains("recipient_id = ?"));
        assertTrue(readAllSql.contains("notification_id <= ?"));
        assertTrue(readAllSql.contains("read_at is null"));

        String notificationCleanupSql = normalize(configuration.getMappedStatement(
                "com.anishan.user.mapper.UserNotificationMapper.deleteExpired")
                .getBoundSql(params).getSql());
        assertTrue(notificationCleanupSql.contains("limit ?"));
        assertTrue(notificationCleanupSql.contains("created_at < ?"));

        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("mapper/NotificationFanoutJobMapper.xml")) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, "NotificationFanoutJobMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        String fanoutCleanupSql = normalize(configuration.getMappedStatement(
                "com.anishan.user.mapper.NotificationFanoutJobMapper.deleteDoneBefore")
                .getBoundSql(params).getSql());
        assertTrue(fanoutCleanupSql.contains("status = 'done'"));
        assertTrue(fanoutCleanupSql.contains("updated_at < ?"));
        assertTrue(fanoutCleanupSql.contains("limit ?"));

        JsonNode unreadCount = objectMapper.readTree(objectMapper.writeValueAsString(
                new com.anishan.user.domain.vo.UnreadCountVo(4L)));
        assertTrue(unreadCount.get("count").isIntegralNumber());
    }

    @Test
    void retentionUsesSpecifiedBoundariesAndBatchLimit() {
        service.deleteExpiredNotificationsBatch();
        service.deleteCompletedFanoutBatch();

        verify(notificationMapper).deleteExpired(NOW.minusDays(180), 500);
        verify(fanoutMapper).deleteDoneBefore(NOW.minusDays(30), 500);

        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("mapper/NotificationFanoutJobMapper.xml")) {
            assertNotNull(input);
            Configuration configuration = new Configuration();
            new XMLMapperBuilder(input, configuration, "NotificationFanoutJobMapper.xml",
                    configuration.getSqlFragments()).parse();
            Map<String, Object> params = new HashMap<>();
            params.put("completedBefore", NOW.minusDays(30));
            params.put("limit", 500);
            String replaySql = normalize(configuration.getMappedStatement(
                    "com.anishan.user.mapper.NotificationFanoutJobMapper.replayFailed")
                    .getBoundSql(Collections.singletonMap("eventId", "event-id")).getSql());
            assertTrue(replaySql.contains("status = 'failed'"));
            assertFalse(replaySql.substring(0, replaySql.indexOf("where")).contains("cursor_user_id"));
            String cleanupSql = normalize(configuration.getMappedStatement(
                    "com.anishan.user.mapper.NotificationFanoutJobMapper.deleteDoneBefore")
                    .getBoundSql(params).getSql());
            assertTrue(cleanupSql.contains("status = 'done'"));
            assertTrue(cleanupSql.contains("updated_at < ?"));
            assertTrue(cleanupSql.contains("limit ?"));
        } catch (Exception exception) {
            throw new AssertionError("Could not validate fanout retention mapper", exception);
        }
    }

    private static UserNotification notification(Long id, Long actorId, String type,
                                                  String action, String reason) {
        UserNotification notification = new UserNotification();
        notification.setNotificationId(id);
        notification.setRecipientId(RECIPIENT_ID);
        notification.setActorId(actorId);
        notification.setType(type);
        notification.setAction(action);
        notification.setReason(reason);
        notification.setOccurredAt(NOW);
        return notification;
    }

    private static NotificationPageQuery query(int size) {
        return query(size, 1L, false);
    }

    private static NotificationPageQuery query(int size, Long page, boolean unreadOnly) {
        NotificationPageQuery query = new NotificationPageQuery();
        query.setPageSize(size);
        query.setCurrentPage(page);
        query.setUnreadOnly(unreadOnly);
        return query;
    }

    private static String normalize(String sql) {
        return sql.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
