package com.anishan.user.controller;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import com.alibaba.nacos.common.utils.UuidUtils;
import com.anishan.commons.enumeration.SseEvent;
import com.anishan.user.domain.dto.UserPoint;
import com.anishan.user.service.SysUserService;
import com.anishan.user.util.SseUtils;
import com.anishan.user.util.UserUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.mail.Session;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Controller
@Api("sse专用接口")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SseController {

    private final SseUtils sseUtils;
    private final SysUserService userService;


    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("sse接口")
    public SseEmitter streamEvents(HttpServletResponse response, @RequestParam String uuid) {

        SseEmitter reconnect = sseUtils.reconnect(uuid);
        if (reconnect != null) {
            return reconnect;
        }

        Long userId = UserUtil.getUserId();

        uuid = IdUtil.fastSimpleUUID();
        SseEmitter sse = sseUtils.createSse(uuid, userId);
        UserPoint point = userService.getPoint(userId);

        Cookie cookie = new Cookie("sse-uuid", uuid);
        cookie.setMaxAge(24 * 60 * 60);
        response.addCookie(cookie);

        sseUtils.sendMessage(uuid, SseEvent.UpdatePoint, point);

        return sse;
    }


}
