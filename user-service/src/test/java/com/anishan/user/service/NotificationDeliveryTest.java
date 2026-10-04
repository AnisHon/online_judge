package com.anishan.user.service;

import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.user.mapper.SysUserMapper;
import com.anishan.user.mapper.UserNotificationMapper;
import com.anishan.user.service.impl.NotificationDeliveryServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDeliveryTest {

    private final UserNotificationMapper notificationMapper = mock(UserNotificationMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationDeliveryServiceImpl service = new NotificationDeliveryServiceImpl(
            notificationMapper, sysUserMapper, objectMapper,
            Clock.fixed(Instant.parse("2026-10-03T04:00:00Z"), ZoneId.of("Asia/Shanghai")));

    @Test
    void repeatedDeliveryAndDifferentEventIdWithSameDedupeCreateOnlyOneRow() {
        Set<String> insertedKeys = new HashSet<>();
        AtomicLong notificationId = new AtomicLong(100L);
        when(sysUserMapper.selectNormalUserIds(anyList())).thenReturn(Collections.singletonList(9L));
        doCallRealMethod().when(notificationMapper).insertNotification(any());
        doAnswer(invocation -> {
            com.anishan.user.domain.entity.UserNotification row = invocation.getArgument(0);
            String key = row.getRecipientId() + ":" + row.getDedupeKey();
            if (!insertedKeys.add(key)) {
                throw new DuplicateKeyException("duplicate notification key");
            }
            row.setNotificationId(notificationId.incrementAndGet());
            return 1;
        }).when(notificationMapper).insertNotificationRow(any());
        when(notificationMapper.selectIdByRecipientAndDedupeForUpdate(9L,
                "solution-liked:2090000000000000001:7")).thenReturn(101L);

        for (int index = 0; index < 10; index++) {
            CommunityEvent duplicate = event().setEventId(UUID.randomUUID().toString());
            service.deliver(duplicate);
        }

        assertEquals(1, insertedKeys.size());
        verify(notificationMapper, times(10)).insertNotificationRow(any());
        verify(notificationMapper, times(9)).selectIdByRecipientAndDedupeForUpdate(9L,
                "solution-liked:2090000000000000001:7");
    }

    @Test
    void moderationCanNotifyItsAuthorEvenWhenTheOperatorIsThatAuthor() {
        when(sysUserMapper.selectNormalUserIds(Collections.singletonList(7L)))
                .thenReturn(Collections.singletonList(7L));
        doCallRealMethod().when(notificationMapper).insertNotification(any());
        when(notificationMapper.insertNotificationRow(any())).thenReturn(1);

        service.deliver(new CommunityEvent().setSchemaVersion(1)
                .setEventId("550e8400-e29b-41d4-a716-446655440000")
                .setEventType(CommunityEventType.SOLUTION_MODERATED)
                .setDedupeKey("solution-moderated:100")
                .setOccurredAt("2026-10-03T12:00:00+08:00")
                .setActorId("7").setSolutionId("2090000000000000001")
                .setRecipientIds(Collections.singletonList("7"))
                .setAction("AUTHOR_ONLY").setReason("需要修改内容"));

        org.mockito.ArgumentCaptor<com.anishan.user.domain.entity.UserNotification> captor =
                org.mockito.ArgumentCaptor.forClass(com.anishan.user.domain.entity.UserNotification.class);
        verify(notificationMapper).insertNotificationRow(captor.capture());
        assertEquals(7L, captor.getValue().getRecipientId());
        assertEquals("需要修改内容", captor.getValue().getReason());
    }

    @Test
    void ordinarySelfNotificationIsRejectedAndNoRecipientCreatesNoRows() {
        CommunityEvent selfLike = event().setRecipientIds(Collections.singletonList("7"));
        assertThrows(IllegalArgumentException.class, () -> service.deliver(selfLike));

        service.deliver(event().setRecipientIds(Collections.emptyList()));

        verify(notificationMapper, never()).insertNotificationRow(any());
        verify(sysUserMapper, never()).selectNormalUserIds(anyList());
    }

    @Test
    void replyEventDeliversToItsDistinctRecipientsOnly() {
        when(sysUserMapper.selectNormalUserIds(Arrays.asList(9L, 10L)))
                .thenReturn(Arrays.asList(9L, 10L));
        doCallRealMethod().when(notificationMapper).insertNotification(any());
        when(notificationMapper.insertNotificationRow(any())).thenReturn(1);
        CommunityEvent reply = new CommunityEvent().setSchemaVersion(1)
                .setEventId("550e8400-e29b-41d4-a716-446655440000")
                .setEventType(CommunityEventType.COMMENT_REPLIED)
                .setDedupeKey("comment-replied:300")
                .setOccurredAt("2026-10-03T12:00:00.123+08:00")
                .setActorId("7").setSolutionId("2090000000000000001").setCommentId("300")
                .setRecipientIds(Arrays.asList("9", "10"));

        service.deliver(reply);

        verify(notificationMapper, times(2)).insertNotificationRow(any());
        verify(notificationMapper, never()).selectIdByRecipientAndDedupeForUpdate(any(), any());
    }

    @Test
    void onlyNormalExistingRecipientsAreInserted() {
        when(sysUserMapper.selectNormalUserIds(Arrays.asList(9L, 10L))).thenReturn(Collections.singletonList(9L));
        doCallRealMethod().when(notificationMapper).insertNotification(any());
        when(notificationMapper.insertNotificationRow(any())).thenReturn(1);

        service.deliver(event().setRecipientIds(Arrays.asList("9", "10")));

        verify(sysUserMapper).selectNormalUserIds(Arrays.asList(9L, 10L));
        verify(notificationMapper, times(1)).insertNotificationRow(any());
    }

    private CommunityEvent event() {
        return new CommunityEvent().setSchemaVersion(1)
                .setEventId("550e8400-e29b-41d4-a716-446655440000")
                .setEventType(CommunityEventType.SOLUTION_LIKED)
                .setDedupeKey("solution-liked:2090000000000000001:7")
                .setOccurredAt("2026-10-03T12:00:00.123+08:00")
                .setActorId("7").setSolutionId("2090000000000000001")
                .setRecipientIds(Collections.singletonList("9"));
    }
}
