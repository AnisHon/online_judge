package com.anishan.problem.controller;

import com.anishan.api.client.gojudge.domain.TestResult;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.problem.domain.dto.ContentProblemReadRequest;
import com.anishan.api.client.problem.domain.vo.JudgeSubmissionStatusVo;
import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.api.util.RedisJudgeTestUtil;
import com.anishan.commons.domain.R;
import com.anishan.problem.service.JudgeResultApplicationService;
import com.anishan.problem.service.ProblemContentReadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/internal")
@Api("内部接口")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InternalController {

    private final RedisJudgeTestUtil redisJudgeTestUtil;
    private final ProblemContentReadService problemContentReadService;
    private final JudgeResultApplicationService judgeResultApplicationService;

    @ApiOperation("读取供内容服务校验的题目元信息")
    @PostMapping("/content-problems/read")
    public R<List<ContentProblemReadVo>> readContentProblemMetadata(
            @RequestBody ContentProblemReadRequest request) {
        return R.success(problemContentReadService.read(request));
    }

    @ApiOperation("更新判题状态，仅供 judge-server 调用")
    @PostMapping("/judgeStatus")
    public R<Void> judgeStatus(@RequestBody JudgeScore judgeScore) {
        judgeResultApplicationService.updateIntermediateStatus(judgeScore);
        return R.success(null);
    }

    @ApiOperation("存储判题结果用的")
    @PostMapping("/judgeResult")
    public R<Void> judgeResult(@RequestBody JudgeScore judgeScore) {
        judgeResultApplicationService.applyFinalResult(judgeScore);
        return R.success(null);
    }

    @ApiOperation("读取判题提交的最小内部状态，不返回源代码")
    @GetMapping("/judge-submission/{submitId}")
    public R<JudgeSubmissionStatusVo> judgeSubmissionStatus(@PathVariable Long submitId) {
        return R.success(judgeResultApplicationService.getSubmissionStatus(submitId));
    }

    @ApiOperation("存储代码测试结果，仅供 judge-server 调用")
    @PostMapping("/testResult")
    public R<Void> testResult(@RequestBody TestResult testResult) {
        if (testResult != null && testResult.getUserId() != null && testResult.getUuid() != null) {
            redisJudgeTestUtil.save(testResult, 120);
        }
        return R.success(null);
    }
}
