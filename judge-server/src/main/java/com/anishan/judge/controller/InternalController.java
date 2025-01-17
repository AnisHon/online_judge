package com.anishan.judge.controller;

import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.OjProblemCaseDto;
import com.anishan.api.client.judgeserver.domain.RunTestInfo;
import com.anishan.api.client.problem.domain.vo.OjProblemCaseVo;
import com.anishan.api.domain.entity.OjProblemCase;
import com.anishan.commons.domain.R;
import com.anishan.judge.service.JudgeService;
import com.anishan.judge.service.OjProblemCaseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
