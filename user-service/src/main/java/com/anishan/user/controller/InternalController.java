package com.anishan.user.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.anishan.api.client.user.domain.SseMessage;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.commons.domain.R;
import com.anishan.user.service.SysUserService;
import com.anishan.user.util.SseUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Api("内部接口，外部请求会被gateway过滤掉")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InternalController {

    private final SysUserService sysUserService;
    private final SseUtils sseUtils;

    @GetMapping("/add-point/{userId}/{point}")
    @ApiOperation("添加用户奖励分")
    public R<Boolean> addPoint(@PathVariable("point") String point, @PathVariable("userId") Long userId) {
        BigDecimal bigDecimal = new BigDecimal(point);
        sysUserService.addPoint(userId, bigDecimal);
        return R.success(true);
    }

    @PostMapping("/send-message")
    @ApiOperation("给用户发送信息，通过SSE")
    public R<Boolean> sendMessage(@RequestBody SseMessage message) {
        boolean b = sseUtils.sendPlainString(message.getUuid(), message.getEvent(), message.getData());
        log.info(message.toString());
        return R.success(b);
    }
    @GetMapping("/nikeName/{ids}")
    @ApiOperation("通过ID获取用户名")
    public R<Map<Long, String>> nikeName(@PathVariable("ids") List<Long> ids) {
        return R.success(sysUserService.getNikeNameToMap(ids));
    }

}
