package com.anishan.judge.mq;

import com.anishan.api.client.gojudge.domain.TestResult;
import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.judgeserver.domain.RunTestInfo;
import com.anishan.api.client.problem.client.ProblemInternalClient;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.judge.judge.JudgeRun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class JudgeListener {

    private final JudgeRun judgeRun;
    private final ProblemInternalClient problemInternalClient;

    private void fillJudgeScore(JudgeScore judgeScore, JudgeInfo judgeInfo) {
        if (judgeScore == null) return;
        judgeScore.setSubmitId(judgeInfo.getSubmitId());
        judgeScore.setProblemId(judgeInfo.getProblemId());
        judgeScore.setUserId(judgeInfo.getUserId());
        judgeScore.setContestId(judgeInfo.getContestId());
        judgeScore.setLanguageId(judgeInfo.getLanguageId());
        judgeScore.setCode(judgeInfo.getCode());
        judgeScore.setLanguageId(judgeInfo.getLanguageId());
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "judge-info-queue"),
                    exchange = @Exchange(name = "judge-exchange"),
                    key = "judge-info"
            )
    )
    public void judge(JudgeInfo info) {

        log.debug("用户ID:{} 开始判题", info.getUserId());

        JudgeScore judge;
        try {
            problemInternalClient.judgeStatus(new JudgeScore()
                    .setSubmitId(info.getSubmitId())
                    .setUserId(info.getUserId())
                    .setResult(JudgeResult.COMPILING));
            // 判题
            judge = judgeRun.judgeAll(info);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            judge = new JudgeScore()
                    .setSubmitId(info.getSubmitId())
                    .setProblemId(info.getProblemId())
                    .setUserId(info.getUserId())
                    .setContestId(info.getContestId())
                    .setCode(info.getCode())
                    .setLanguageId(info.getLanguageId())
                    .setResult(JudgeResult.JUDGE_ERROR)
                    .setErrorCode("JUDGE_PIPELINE_EXCEPTION")
                    .setInternalError(e.getClass().getName() + ": " + e.getMessage());
        }

        if (judge == null) {
            judge = new JudgeScore()
                    .setResult(JudgeResult.JUDGE_ERROR)
                    .setErrorCode("EMPTY_JUDGE_RESULT")
                    .setInternalError("judge runner returned null");
        }
        fillJudgeScore(judge, info);
        // problem-service 是公开提交日志和 records 的唯一落库方，避免 judge-server 写出重复行。
        problemInternalClient.judgeResult(judge);


        log.debug("用户ID:{} 判题结束", info.getUserId());
    }


    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "test-info-queue"),
                    exchange = @Exchange(name = "judge-exchange"),
                    key = "test-info"
            )
    )
    public void test(RunTestInfo info) {

        log.debug("用户ID:{} 开始测试", info.getUserId());

        // 判题
//        TestResult testResult = judgeService.test(info);
        TestResult testResult;
        try {
            testResult = judgeRun.judgeTest(info);
        } catch (Exception e) {

             testResult = new TestResult()
                     .setJudgeResult(JudgeResult.JUDGE_ERROR);
        }

        if (testResult == null) {
            testResult = new TestResult().setJudgeResult(JudgeResult.JUDGE_ERROR);
        }
        testResult.setUserId(info.getUserId()).setUuid(info.getUuid());
        problemInternalClient.testResult(testResult);
        log.debug("用户ID:{} 测试结束", info.getUserId());
    }


}
