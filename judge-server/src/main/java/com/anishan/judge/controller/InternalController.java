package com.anishan.judge.controller;

import com.anishan.api.client.judgeserver.domain.JudgeMessage;
import com.anishan.commons.domain.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@Api("内部接口，需要路由拦截，不可向外暴露")
public class InternalController {

    @ApiOperation("判题接口")
    @PostMapping("/judge")
    public R<Void> judge(JudgeMessage judgeMessage) {
        return R.success(null);
    }



}
