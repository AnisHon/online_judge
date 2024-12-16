package com.anishan.judge.controller;

import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.JudgeMessage;
import com.anishan.api.client.problem.domain.vo.OjProblemCaseVo;
import com.anishan.commons.domain.R;
import com.anishan.judge.service.JudgeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal")
@Api("内部接口，需要路由拦截，不可向外暴露")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InternalController {


    private final JudgeService
            judgeService;


    @ApiOperation("")
    @GetMapping("/getCases/{problemId}")
    public R<List<OjProblemCaseVo>> getCases(@PathVariable Long problemId) {

    }


    @ApiOperation("判题接口")
    @PostMapping("/judge")
    public R<Long> judge(@RequestBody JudgeInfo judgeInfo) {
        judgeService.sendJudgeMessage(judgeInfo);
        return R.success(null);
    }



}
