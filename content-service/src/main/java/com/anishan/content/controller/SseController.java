package com.anishan.content.controller;

import cn.hutool.core.util.IdUtil;
import com.anishan.commons.domain.R;
import com.anishan.commons.enumeration.SseEvent;
import com.anishan.content.service.SseService;
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

    private final SseService sseService;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("sse接口")
    public SseEmitter streamEvents(@RequestHeader("user-id") Long userId, @RequestParam String uuid) {

        SseEmitter reconnect = sseService.reconnect(uuid);
        if (reconnect != null) {
            return reconnect;
        }

        uuid = IdUtil.fastSimpleUUID();
        SseEmitter sse = sseService.createSse(uuid, userId);

        sseService.sendMessage(uuid, SseEvent.SET_UUID, uuid);

        return sse;
    }


    @ResponseBody
    @DeleteMapping("/close/{uuid}")
    public R<Void> close(@PathVariable String uuid) {
        sseService.close(uuid);
        return R.success(null);
    }

}
