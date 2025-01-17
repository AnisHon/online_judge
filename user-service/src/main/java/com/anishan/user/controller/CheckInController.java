package com.anishan.user.controller;

import com.anishan.api.annotation.EnableCache;
import com.anishan.commons.domain.R;
import com.anishan.user.domain.dto.UserCheckInDto;
import com.anishan.user.domain.vo.UserCheckInInfo;
import com.anishan.user.service.AuthenticationService;
import com.anishan.user.service.UserCheckInService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/check-in")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Api("签到接口")
public class CheckInController {

    private final UserCheckInService userCheckInService;
    private final AuthenticationService authenticationService;
    @GetMapping
    @ApiOperation("签到")
    public R<UserCheckInInfo> checkIn() {
        UserCheckInInfo data = userCheckInService.checkIn(authenticationService.myId());
        return R.success(data);
    }

    @GetMapping("/list")
    @ApiOperation("列出最近签到列表")
    @EnableCache(name = "check-in-list", expire = 60 * 1000)
    public R<List<UserCheckInDto>> list() {
        return R.success(userCheckInService.getUserCheckInList());
    }

    @GetMapping("/already")
    @ApiOperation("查询是否签到了")
    public R<Boolean> already() {
        boolean checkedIn = userCheckInService.isCheckedIn(authenticationService.myId());
        return R.success(checkedIn);
    }

    @GetMapping("/today")
    @ApiOperation("今天的签到人数")
    public R<Long> today() {
        return R.success(userCheckInService.todayCount());
    }




}
