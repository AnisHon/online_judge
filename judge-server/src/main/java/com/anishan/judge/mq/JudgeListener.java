package com.anishan.judge.mq;

import com.anishan.api.client.content.client.ContentInternalClient;
import com.anishan.api.client.gojudge.domain.TestResult;
import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.judgeserver.domain.RunTestInfo;
import com.anishan.api.client.problem.client.ProblemInternalClient;
import com.anishan.api.client.user.domain.SseMessage;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.commons.enumeration.SseEvent;
import com.anishan.judge.domain.entity.SubmitLog;
import com.anishan.judge.service.JudgeService;
import com.anishan.judge.service.SubmitLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class JudgeListener {

    private final JudgeService judgeService;
    private final SubmitLogService submitLogService;
    private final ContentInternalClient clientInternalClient;
    private final ProblemInternalClient problemInternalClient;


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

    private void notifyCompiling(String uuid) {
        notify(uuid, JudgeResult.Compiling, "");
    }

    private void notify(String uuid, JudgeResult result, String stderr) {
        notify(uuid, result, "", stderr);
    }

    /**
     * 需要注意一下顺序问题，这里stdin是第三个
     */
    private void notify(String uuid, JudgeResult result, String stdout, String stderr) {
        HashMap<String, String> map = new HashMap<>();
        map.put("state", result.value());
        map.put("stderr", stderr);
        map.put("stdout", stdout);

        SseMessage sseMessage = SseMessage.create(uuid, SseEvent.UpdateJudgeState, map);
        clientInternalClient.sendMessage(sseMessage);
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

        // 通知编译
        notifyCompiling(info.getUuid());

        // 判题
        JudgeScore judge = judgeService.judge(info);

        // 记录提交日志
        logSubmit(judge, info);


        // 提交Record信息
        fillJudgeScore(judge, info);
        problemInternalClient.judgeResult(judge);

        // 通知完成
        JudgeResult result = judge == null ? JudgeResult.RuntimeError : judge.getResult();
        String stderr = judge == null ? "" : judge.getErrorMessage();

        notify(info.getUuid(), result, stderr);

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

        // 通知编译
        notifyCompiling(info.getUuid());
        // 判题
        TestResult testResult = judgeService.test(info);

        // 通知完成
        notify(info.getUuid(), testResult.getJudgeResult(), testResult.getStdout(), testResult.getStderr());

        log.debug("用户ID:{} 测试结束", info.getUserId());
    }


}
