package com.anishan.problem.controller;

import com.anishan.commons.domain.R;
import com.anishan.problem.domain.dto.JudgeRequest;
import com.anishan.problem.domain.vo.ProblemJudgeResult;
import com.anishan.problem.service.JudgeService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/judge")
@ApiModel("判题模块")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JudgeController {


    private final JudgeService judgeService;

    @PostMapping
    @ApiOperation("判题")
    public R<ProblemJudgeResult> judge(
            @RequestHeader("user-id") Long userId,
            @RequestBody @Validated JudgeRequest judgeRequest
    ) {

        ProblemJudgeResult judge = judgeService.judge(userId, judgeRequest);
        return R.success(judge);

    }

    @PostMapping("/test")
    @ApiOperation("OJ代码测试运行")
    public R<ProblemJudgeResult> test(@RequestBody JudgeRequest judgeRequest) {

        ProblemJudgeResult judge = judgeService.codeTest(judgeRequest);
        return R.success(judge);

    }







}
