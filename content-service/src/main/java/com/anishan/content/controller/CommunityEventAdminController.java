package com.anishan.content.controller;

import com.anishan.api.util.AuthUtil;
import com.anishan.commons.domain.R;
import com.anishan.content.service.ContentEventOutboxService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@Api("社区事件管理")
@RestController
@RequestMapping("/solution/admin/events")
@RequiredArgsConstructor
@Slf4j
public class CommunityEventAdminController {

    private final ContentEventOutboxService contentEventOutboxService;

    @PostMapping("/{eventId}/replay")
    @PreAuthorize("hasAuthority('problem:solution:edit')")
    @ApiOperation("重放失败的社区题解事件")
    public R<Map<String, Boolean>> replay(@PathVariable String eventId) {
        String eventType = contentEventOutboxService.replayCommunityFailed(eventId);
        log.info("Community event replay accepted eventId={} eventType={} operatorId={}",
                eventId, eventType, AuthUtil.getUserId());
        return R.success(Collections.singletonMap("accepted", true));
    }
}
