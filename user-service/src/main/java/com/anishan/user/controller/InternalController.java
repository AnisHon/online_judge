package com.anishan.user.controller;

import com.anishan.commons.domain.R;
import com.anishan.user.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Api("内部接口，外部请求会被gateway过滤掉")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InternalController {

    private final SysUserService sysUserService;

    @GetMapping("/add-point/{userId}/{point}")
    @ApiOperation("添加用户奖励分")
    public R<Boolean> addPoint(@PathVariable("point") String point, @PathVariable("userId") Long userId) {
        BigDecimal bigDecimal = new BigDecimal(point);
        sysUserService.addPoint(userId, bigDecimal);
        return R.success(true);
    }

    @GetMapping("/nikeName/{ids}")
    @ApiOperation("通过ID获取用户名")
    public R<Map<Long, String>> nikeName(@PathVariable("ids") List<Long> ids) {
        return R.success(sysUserService.getNikeNameToMap(ids));
    }

}
