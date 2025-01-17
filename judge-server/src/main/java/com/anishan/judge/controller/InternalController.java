package com.anishan.judge.controller;

import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.RunTestInfo;
import com.anishan.commons.domain.R;
import com.anishan.judge.service.JudgeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/internal")
@Api("内部接口，需要路由拦截，不可向外暴露")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InternalController {


    private final JudgeService judgeService;

    @ApiOperation("判题接口")
    @PostMapping("/judge")
    public R<Boolean> judge(@RequestBody JudgeInfo judgeInfo) {
        boolean b = judgeService.sendJudgeMessage(judgeInfo);
        return R.success(b);
    }

    @ApiOperation("测试代码接口")
    @PostMapping("/test")
    public R<Boolean> test(@RequestBody RunTestInfo runTestInfo) {
        boolean b = judgeService.sendTestMessage(runTestInfo);
        return R.success(b);
    }



}
