package com.anishan.user.controller;

import com.anishan.commons.e.SseEvent;
import com.anishan.user.domain.dto.UserPoint;
import com.anishan.user.service.SysUserService;
import com.anishan.user.util.SseUtils;
import com.anishan.user.util.UserUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@Api("sse专用接口")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SseController {

    private final SseUtils sseUtils;
    private final SysUserService userService;


    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("sse接口")
    public SseEmitter streamEvents(HttpServletResponse response) throws IOException, InterruptedException {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");

        Long userId = UserUtil.getUserId();

        SseEmitter sse = sseUtils.createSse(userId);
        UserPoint point = userService.getPoint(userId);
        sseUtils.sendMessage(userId, SseEvent.UpdatePoint, point);
        System.out.println(point);
        return sse;
    }
}
