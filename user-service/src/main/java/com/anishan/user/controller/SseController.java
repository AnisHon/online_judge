package com.anishan.user.controller;

import cn.hutool.core.util.IdUtil;
import com.anishan.commons.domain.R;
import com.anishan.commons.enumeration.SseEvent;
import com.anishan.user.domain.dto.UserPoint;
import com.anishan.user.service.SysUserService;
import com.anishan.user.util.SseUtils;
import com.anishan.user.util.UserUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@Api("sse专用接口")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RequestMapping("/sse")
public class SseController {

    private final SseUtils sseUtils;
    private final SysUserService userService;


    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("sse接口")
    public SseEmitter streamEvents(@RequestParam String uuid) {

        SseEmitter reconnect = sseUtils.reconnect(uuid);
        if (reconnect != null) {
            return reconnect;
        }

        Long userId = UserUtil.getUserId();

        uuid = IdUtil.fastSimpleUUID();
        SseEmitter sse = sseUtils.createSse(uuid, userId);
        UserPoint point = userService.getPoint(userId);

        sseUtils.sendMessage(uuid, SseEvent.UpdatePoint, point);
        sseUtils.sendMessage(uuid, SseEvent.SetUUID, uuid);

        return sse;
    }


    @ResponseBody
    @DeleteMapping("/close/{uuid}")
    public R<Void> close(@PathVariable String uuid) {
        sseUtils.close(uuid);
        return R.success(null);
    }

}
