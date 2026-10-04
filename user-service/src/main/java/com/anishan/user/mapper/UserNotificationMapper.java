package com.anishan.user.mapper;

import com.anishan.user.domain.entity.UserNotification;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** Queries and updates are always scoped to the notification recipient. */
@Mapper
public interface UserNotificationMapper {

    default int insertNotification(UserNotification notification) {
        if (notification.getNotificationId() == null) {
            notification.setNotificationId(IdWorker.getId());
        }
        return insertNotificationRow(notification);
    }

    int insertNotificationRow(@Param("notification") UserNotification notification);

    Long selectIdByRecipientAndDedupeForUpdate(@Param("recipientId") Long recipientId,
                                               @Param("dedupeKey") String dedupeKey);

    List<UserNotification> selectPageByRecipient(@Param("recipientId") Long recipientId,
                                                 @Param("unreadOnly") boolean unreadOnly,
                                                 @Param("offset") Long offset,
                                                 @Param("pageSize") Integer pageSize);

    long countByRecipient(@Param("recipientId") Long recipientId,
                          @Param("unreadOnly") boolean unreadOnly);

    int existsByRecipientAndId(@Param("recipientId") Long recipientId,
                               @Param("notificationId") Long notificationId);

    long countUnread(@Param("recipientId") Long recipientId);

    int markRead(@Param("notificationId") Long notificationId,
                 @Param("recipientId") Long recipientId,
                 @Param("readAt") LocalDateTime readAt);

    int markAllRead(@Param("recipientId") Long recipientId,
                    @Param("throughId") Long throughId,
                    @Param("readAt") LocalDateTime readAt);

    int deleteExpired(@Param("createdBefore") LocalDateTime createdBefore,
                      @Param("limit") Integer limit);
}
