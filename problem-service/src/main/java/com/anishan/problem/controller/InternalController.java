package com.anishan.problem.controller;

import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.judgeserver.domain.JudgeCaseResult;
import com.anishan.api.client.gojudge.domain.TestResult;
import com.anishan.api.util.RedisJudgeTestUtil;
import com.anishan.api.util.RedisJudgeSubmissionLock;
import com.anishan.commons.domain.R;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.problem.domain.entity.Records;
import com.anishan.problem.domain.vo.UserAnswer;
import com.anishan.problem.service.RecordsService;
import com.anishan.problem.service.SubmitLogService;
import com.anishan.problem.service.JudgeCaseLogService;
import com.anishan.problem.domain.entity.JudgeCaseLog;
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
@Api("内部接口")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InternalController {

    private final RecordsService recordsService;
    private final SubmitLogService submitLogService;
    private final JudgeCaseLogService judgeCaseLogService;
    private final RedisJudgeTestUtil redisJudgeTestUtil;
    private final RedisJudgeSubmissionLock redisJudgeSubmissionLock;

    @ApiOperation("更新判题状态，仅供 judge-server 调用")
    @PostMapping("/judgeStatus")
    public R<Void> judgeStatus(@RequestBody JudgeScore judgeScore) {
        submitLogService.updateStatus(judgeScore);
        return R.success(null);
    }

    @ApiOperation("存储判题结果用的")
    @PostMapping("/judgeResult")
    public R<Void> judgeResult(@RequestBody JudgeScore judgeScore) {
        try {
            JudgeResult finalResult = judgeScore.getResult() == null
                    ? JudgeResult.JUDGE_ERROR : judgeScore.getResult();
            judgeScore.setResult(finalResult);
            submitLogService.complete(judgeScore);

            if (judgeScore.getCaseResults() != null && judgeScore.getSubmitId() != null) {
                // Rabbit 重试时保证内部日志幂等，避免同一提交产生两套 case 日志。
                judgeCaseLogService.remove(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<JudgeCaseLog>()
                        .eq(JudgeCaseLog::getSubmitId, judgeScore.getSubmitId()));
                judgeCaseLogService.saveBatch(judgeScore.getCaseResults().stream()
                        .map(result -> toCaseLog(judgeScore, result))
                        .collect(java.util.stream.Collectors.toList()));
            }

            // 判题基础设施异常不应把题目标记为“已完成”；普通 CE/WA/RE 仍按一次正常提交记录。
            if (finalResult != JudgeResult.JUDGE_ERROR) {
                UserAnswer userAnswer = new UserAnswer();
                userAnswer.setCode(judgeScore.getCode());
                userAnswer.setLanguageId(judgeScore.getLanguageId());

                Records records = new Records();
                records
                        .setContestId(judgeScore.getContestId())
                        .setProblemId(judgeScore.getProblemId())
                        .setUserId(judgeScore.getUserId())
                        .setScore(judgeScore.getScore())
                        .setStatus(judgeScore.getResult() == JudgeResult.ACCEPT)
                        .setAnswer(userAnswer);

                recordsService.addRecord(records);
            }

            return R.success(null);
        } finally {
            redisJudgeSubmissionLock.release(judgeScore.getUserId(), judgeScore.getSubmissionLockToken());
        }
    }

    @ApiOperation("存储代码测试结果，仅供 judge-server 调用")
    @PostMapping("/testResult")
    public R<Void> testResult(@RequestBody TestResult testResult) {
        if (testResult != null && testResult.getUserId() != null && testResult.getUuid() != null) {
            redisJudgeTestUtil.save(testResult, 120);
        }
        return R.success(null);
    }

    private JudgeCaseLog toCaseLog(JudgeScore score, JudgeCaseResult result) {
        return new JudgeCaseLog()
                .setSubmitId(score.getSubmitId())
                .setProblemId(score.getProblemId())
                .setCaseId(result.getCaseId())
                .setCaseIndex(result.getCaseIndex())
                .setStatus(result.getStatus() == null ? JudgeResult.JUDGE_ERROR : result.getStatus())
                .setScore(result.getScore())
                .setTime(result.getTime())
                .setMemory(result.getMemory())
                .setInternalError(result.getErrorMessage());
    }


}
