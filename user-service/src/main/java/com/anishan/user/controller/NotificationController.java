package com.anishan.user.controller;

import com.anishan.api.util.AuthUtil;
import com.anishan.commons.domain.R;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.user.domain.dto.NotificationPageQuery;
import com.anishan.user.domain.dto.NotificationReadAllRequest;
import com.anishan.user.domain.vo.NotificationVo;
import com.anishan.user.domain.vo.UnreadCountVo;
import com.anishan.user.service.NotificationFanoutService;
import com.anishan.user.service.NotificationQueryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

@Api("个人消息收件箱")
@RestController
@RequestMapping("/notification")
@Validated
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationQueryService notificationQueryService;
    private final NotificationFanoutService notificationFanoutService;

    @GetMapping("/page")
    @ApiOperation("分页查看本人消息")
    public R<PagedResult<NotificationVo>> page(@Valid NotificationPageQuery query) {
        return R.success(notificationQueryService.page(AuthUtil.getUserId(), query));
    }

    @GetMapping("/unread-count")
    @ApiOperation("查看本人未读消息数量")
    public R<UnreadCountVo> unreadCount() {
        return R.success(notificationQueryService.unreadCount(AuthUtil.getUserId()));
    }

    @PutMapping("/{notificationId}/read")
    @ApiOperation("标记本人一条消息为已读")
    public R<Map<String, Boolean>> markRead(@PathVariable Long notificationId) {
        notificationQueryService.markRead(AuthUtil.getUserId(), notificationId);
        return R.success(Collections.singletonMap("read", true));
    }

    @PutMapping("/read-all")
    @ApiOperation("标记本人已看到的消息为已读")
    public R<Map<String, Integer>> markAllRead(@Valid @RequestBody NotificationReadAllRequest request) {
        int updatedCount = notificationQueryService.markAllRead(AuthUtil.getUserId(), request.getThroughId());
        return R.success(Collections.singletonMap("updatedCount", updatedCount));
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/fanout/{eventId}/replay")
    @PreAuthorize("hasAuthority('user:user:edit')")
    @ApiOperation("重放失败的题解发布粉丝通知")
    public R<Map<String, Boolean>> replayFanout(@PathVariable String eventId) {
        notificationFanoutService.replayFailed(eventId);
        log.info("Fanout replay accepted eventId={} operatorId={} eventType=SOLUTION_PUBLISHED",
                eventId, AuthUtil.getUserId());
        return R.success(Collections.singletonMap("accepted", true));
    }
}
