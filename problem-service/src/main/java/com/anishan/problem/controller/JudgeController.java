package com.anishan.problem.controller;

import com.anishan.api.client.gojudge.domain.TestResult;
import com.anishan.commons.domain.R;
import com.anishan.problem.domain.dto.JudgeRequest;
import com.anishan.problem.domain.dto.TestRequest;
import com.anishan.problem.domain.vo.ProblemJudgeResult;
import com.anishan.problem.service.ContestService;
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
    private final ContestService contestService;

    @PostMapping
    @ApiOperation("判题")
    public R<Object> judge(
            @RequestHeader("user-id") Long userId,
            @RequestBody @Validated JudgeRequest judgeRequest
    ) {

        Long contestId = judgeRequest.getContestId();
        if (contestId != null) {
            boolean status = contestService.getStatus(userId, contestId);
            if (!status) {
                return R.badRequest("已经不能交卷了");
            }
        }

        ProblemJudgeResult judge = judgeService.judge(userId, judgeRequest);
        return R.success(judge);

    }

    @PostMapping("/test")
    @ApiOperation("OJ代码测试运行")
    public R<String> test(@RequestHeader("user-id") Long userId,@RequestBody TestRequest testRequest) {
        judgeService.codeTest(userId, testRequest);
        return R.success();
    }


    @GetMapping("/test-status")
    @ApiOperation("OJ代码测试结果查看")
    public R<TestResult> testStatus(@RequestHeader("user-id") Long userId,
                                    @RequestParam("uuid") String uuid) {
        TestResult testResult = judgeService.testStatus(userId, uuid);
        return R.success(testResult);
    }






}
