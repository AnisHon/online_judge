package com.anishan.judge.mq;

import com.anishan.api.client.gojudge.domain.TestResult;
import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.judgeserver.domain.RunTestInfo;
import com.anishan.api.client.problem.client.ProblemInternalClient;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.judge.domain.entity.SubmitLog;
import com.anishan.judge.judge.Judge;
import com.anishan.judge.judge.JudgeRun;
import com.anishan.judge.service.SubmitLogService;
import com.anishan.judge.util.JudgeNotifyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.util.Optional;


@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class JudgeListener {

    private final JudgeRun judgeRun;
    private final SubmitLogService submitLogService;
    private final ProblemInternalClient problemInternalClient;
    private final JudgeNotifyUtil judgeNotifyUtil;


    private void logSubmit(JudgeScore judge, JudgeInfo judgeInfo) {
        SubmitLog submitLog = new SubmitLog()
                .setUserId(judgeInfo.getUserId())
                .setProblemId(judgeInfo.getProblemId())
                .setLanguage(judgeInfo.getLanguage());




        if (judge != null) {
            submitLog
                    .setStatus(judge.getResult())
                    .setTime(judge.getRuntime())
                    .setMemory(judge.getMemory())
                    .setStderr(judge.getErrorMessage());
        }
        submitLogService.save(submitLog);
    }


    private void fillJudgeScore(JudgeScore judgeScore, JudgeInfo judgeInfo) {
        if (judgeScore == null) {
            return;
        }

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

        JudgeScore judge = null;
        try {
            // 判题
            judge = judgeRun.judgeAll(info);
            // 记录提交日志
            logSubmit(judge, info);
            // 提交Record信息
            fillJudgeScore(judge, info);
            problemInternalClient.judgeResult(judge);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }


        // 通知完成
        JudgeResult result = judge == null ? JudgeResult.RUNTIME_ERROR : judge.getResult();
        String stderr = judge == null ? "" : judge.getErrorMessage();

        // 没有结果，可能是人为因素
        result = Optional.ofNullable(result).orElse(JudgeResult.RUNTIME_ERROR);

        judgeNotifyUtil.notify(info.getUuid(), result, stderr);

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
                     .setJudgeResult(JudgeResult.RUNTIME_ERROR);
        }
        // 通知完成
        judgeNotifyUtil.notify(info.getUuid(), testResult.getJudgeResult(), testResult.getStdout(), testResult.getStderr());

        log.debug("用户ID:{} 测试结束", info.getUserId());
    }


}
