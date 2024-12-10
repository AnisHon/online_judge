package com.anishan.user.controller;

import com.anishan.commons.domain.R;
import com.anishan.user.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Api("内部接口，外部请求会被gateway过滤掉")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class IntervalController {

    private final SysUserService sysUserService;

    @GetMapping("/add-point/{userId}/{point}")
    @ApiOperation("添加用户奖励分")
    public R<Boolean> addPoint(@PathVariable("point") String point, @PathVariable("userId") Long userId) {
        BigDecimal bigDecimal = new BigDecimal(point);
        sysUserService.addPoint(userId, bigDecimal);
        return R.success(true);
    }

}
