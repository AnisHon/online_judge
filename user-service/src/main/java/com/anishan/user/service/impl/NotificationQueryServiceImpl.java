package com.anishan.user.service.impl;

import com.anishan.api.client.user.domain.vo.UserSummaryVo;
import com.anishan.api.event.CommunityEventType;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.user.domain.dto.NotificationPageQuery;
import com.anishan.user.domain.entity.UserNotification;
import com.anishan.user.domain.vo.NotificationVo;
import com.anishan.user.domain.vo.UnreadCountVo;
import com.anishan.user.mapper.NotificationFanoutJobMapper;
import com.anishan.user.mapper.UserNotificationMapper;
import com.anishan.user.service.NotificationQueryService;
import com.anishan.user.service.SysUserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int RETENTION_BATCH_SIZE = 500;
    private static final Set<CommunityEventType> KNOWN_TYPES =
            Collections.unmodifiableSet(EnumSet.allOf(CommunityEventType.class));

    private final UserNotificationMapper notificationMapper;
    private final NotificationFanoutJobMapper fanoutJobMapper;
    private final SysUserService sysUserService;
    private final Clock clock;

    public NotificationQueryServiceImpl(UserNotificationMapper notificationMapper,
                                         NotificationFanoutJobMapper fanoutJobMapper,
                                         SysUserService sysUserService,
                                         @Qualifier("userCommunityClock") Clock clock) {
        this.notificationMapper = notificationMapper;
        this.fanoutJobMapper = fanoutJobMapper;
        this.sysUserService = sysUserService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<NotificationVo> page(Long recipientId, NotificationPageQuery query) {
        requireRecipient(recipientId);
        if (query == null || query.getCurrentPage() == null || query.getCurrentPage() < 1
                || query.getPageSize() == null || query.getPageSize() < 1
                || query.getPageSize() > MAX_PAGE_SIZE) {
            throw new ApiStatusException(400, "分页参数不合法");
        }

        final long offset;
        try {
            offset = Math.multiplyExact(query.getCurrentPage() - 1L, query.getPageSize().longValue());
        } catch (ArithmeticException overflow) {
            throw new ApiStatusException(400, "分页参数超出范围");
        }

        boolean unreadOnly = Boolean.TRUE.equals(query.getUnreadOnly());
        List<UserNotification> rows = notificationMapper.selectPageByRecipient(
                recipientId, unreadOnly, offset, query.getPageSize());
        long total = notificationMapper.countByRecipient(recipientId, unreadOnly);
        if (rows == null || rows.isEmpty()) {
            return PagedResult.fromPage(new Page<>(query.getCurrentPage(), query.getPageSize()),
                    Collections.emptyList(), total);
        }

        Set<Long> actorIds = new LinkedHashSet<>();
        for (UserNotification row : rows) {
            if (row.getActorId() != null && row.getActorId() > 0) {
                actorIds.add(row.getActorId());
            }
        }
        Map<Long, UserSummaryVo> actorsById = new LinkedHashMap<>();
        if (!actorIds.isEmpty()) {
            List<UserSummaryVo> summaries = sysUserService.getUserSummariesByIds(new ArrayList<>(actorIds));
            if (summaries != null) {
                for (UserSummaryVo summary : summaries) {
                    if (summary != null && summary.getUserId() != null) {
                        actorsById.put(summary.getUserId(), summary);
                    }
                }
            }
        }

        List<NotificationVo> result = new ArrayList<>(rows.size());
        for (UserNotification row : rows) {
            result.add(toVo(row, actorsById.get(row.getActorId())));
        }
        return PagedResult.fromPage(new Page<>(query.getCurrentPage(), query.getPageSize()), result, total);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountVo unreadCount(Long recipientId) {
        requireRecipient(recipientId);
        return new UnreadCountVo(notificationMapper.countUnread(recipientId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long recipientId, Long notificationId) {
        requireRecipient(recipientId);
        if (notificationId == null || notificationId <= 0) {
            throw new ApiStatusException(404, "消息不存在");
        }
        if (notificationMapper.markRead(notificationId, recipientId, LocalDateTime.now(clock)) != 1
                && notificationMapper.existsByRecipientAndId(recipientId, notificationId) != 1) {
            throw new ApiStatusException(404, "消息不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markAllRead(Long recipientId, String throughIdText) {
        requireRecipient(recipientId);
        final long throughId;
        try {
            if (throughIdText == null || !throughIdText.matches("[1-9][0-9]{0,18}")) {
                throw new NumberFormatException("invalid notification id");
            }
            throughId = Long.parseLong(throughIdText);
        } catch (NumberFormatException invalid) {
            throw new ApiStatusException(400, "消息范围ID不合法");
        }
        return notificationMapper.markAllRead(recipientId, throughId, LocalDateTime.now(clock));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteExpiredNotificationsBatch() {
        return notificationMapper.deleteExpired(LocalDateTime.now(clock).minusDays(180), RETENTION_BATCH_SIZE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCompletedFanoutBatch() {
        return fanoutJobMapper.deleteDoneBefore(LocalDateTime.now(clock).minusDays(30), RETENTION_BATCH_SIZE);
    }

    private static NotificationVo toVo(UserNotification row, UserSummaryVo actor) {
        NotificationVo vo = new NotificationVo();
        vo.setNotificationId(row.getNotificationId());
        vo.setActor(actor);
        vo.setSolutionId(row.getSolutionId());
        vo.setCommentId(row.getCommentId());
        vo.setOccurredAt(row.getOccurredAt());
        vo.setReadAt(row.getReadAt());

        CommunityEventType type = knownType(row.getType());
        vo.setType(type == null ? "UNKNOWN" : type.name());
        if (type == CommunityEventType.SOLUTION_MODERATED) {
            if ("AUTHOR_ONLY".equals(row.getAction()) || "RESTORE".equals(row.getAction())
                    || "DELETE".equals(row.getAction())) {
                vo.setAction(row.getAction());
                vo.setReason(row.getReason());
            }
        } else if (type == CommunityEventType.COMMENT_MODERATED && "DELETE".equals(row.getAction())) {
            vo.setAction(row.getAction());
            vo.setReason(row.getReason());
        }
        return vo;
    }

    private static CommunityEventType knownType(String value) {
        if (value == null) {
            return null;
        }
        try {
            CommunityEventType type = CommunityEventType.valueOf(value);
            return KNOWN_TYPES.contains(type) ? type : null;
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    private static void requireRecipient(Long recipientId) {
        if (recipientId == null || recipientId <= 0) {
            throw new ApiStatusException(401, "请先登录");
        }
    }
}
