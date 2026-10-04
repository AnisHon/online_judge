package com.anishan.user.service;

import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.user.domain.dto.NotificationPageQuery;
import com.anishan.user.domain.vo.NotificationVo;
import com.anishan.user.domain.vo.UnreadCountVo;

public interface NotificationQueryService {

    PagedResult<NotificationVo> page(Long recipientId, NotificationPageQuery query);

    UnreadCountVo unreadCount(Long recipientId);

    void markRead(Long recipientId, Long notificationId);

    int markAllRead(Long recipientId, String throughId);

    int deleteExpiredNotificationsBatch();

    int deleteCompletedFanoutBatch();
}
