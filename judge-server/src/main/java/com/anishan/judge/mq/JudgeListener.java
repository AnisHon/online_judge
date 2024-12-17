package com.anishan.judge.mq;

import com.anishan.api.client.gojudge.domain.TestResult;
import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.JudgeMessage;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.judgeserver.domain.RunTestInfo;
import com.anishan.api.client.problem.client.ProblemInternalClient;
import com.anishan.api.client.problem.client.RecordClient;
import com.anishan.api.client.problem.client.SubmitLogClient;
import com.anishan.api.client.problem.domain.dto.SubmitLogDto;
import com.anishan.api.client.user.client.UserInternalClient;
import com.anishan.api.client.user.domain.SseMessage;
import com.anishan.api.util.RedisJudgeTestUtil;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.commons.enumeration.SseEvent;
import com.anishan.judge.domain.entity.SubmitLog;
import com.anishan.judge.exception.SubmitError;
import com.anishan.judge.exception.SystemError;
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
    private final SubmitLogClient submitLogClient;
    private final RecordClient recordClient;
    private final RedisJudgeTestUtil redisJudgeTestUtil;
    private final SubmitLogService submitLogService;
    private final UserInternalClient userInternalClient;
    private final ProblemInternalClient problemInternalClient;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "judge-queue"),
                    exchange = @Exchange(name = "judge-exchange"),
                    key = "judge"
            )
    )
    public void judge(JudgeMessage message) {

        JudgeScore judgeScore;

        submitLogClient.changeStatus(
                new SubmitLogDto()
                        .setSubmitId(message.getSubmitId())
                        .setStatus(JudgeResult.Compiling),
                message.getUserId()
                );

        try {
            judgeScore = judgeService.judge(message);
            judgeScore.setCode(message.getCode());
            judgeScore.setLanguageId(message.getLanguageId());
        } catch (SystemError | SubmitError e) {
           log.error("判题机出错");
           log.error(e.getMessage(), e);
           return;
        } catch (RuntimeException e) {
            log.error("判题机收到非法语言：{}", e.getMessage());
            return;
        }



        SubmitLogDto submitLogDto =
                new SubmitLogDto()
                        .setSubmitId(message.getSubmitId())
                        .setUserId(message.getUserId())
                        .setProblemId(message.getProblemId())
                        .setStatus(judgeScore.getResult())
                        .setTime(judgeScore.getRuntime())
                        .setMemory(judgeScore.getMemory())
                        .setStderr(judgeScore.getErrorMessage())
                        .setLanguage(message.getLanguage());


        // 更新日志状态
        submitLogClient.update(submitLogDto, message.getUserId());

        // 更新record
        recordClient.judgeSave(judgeScore, message.getUserId());



    }


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
        userInternalClient.sendMessage(sseMessage);
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

        // 通知编译
        notifyCompiling(info.getUuid());

        // 判题
        JudgeScore judge = judgeService.judge(info);



        // 通知完成
        JudgeResult result = judge == null ? JudgeResult.RuntimeError : judge.getResult();
        String stderr = judge == null ? "" : judge.getErrorMessage();

        notify(info.getUuid(), result, stderr);

        // 记录提交日志
        logSubmit(judge, info);


        // 提交Record信息
        fillJudgeScore(judge, info);
        problemInternalClient.judgeResult(judge);

    }


    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "test-info-queue"),
                    exchange = @Exchange(name = "judge-exchange"),
                    key = "test-info"
            )
    )
    public void test(RunTestInfo info) {
        // 通知编译
        notifyCompiling(info.getUuid());
        // 判题
        TestResult testResult = judgeService.test(info);

        // 通知完成
        notify(info.getUuid(), testResult.getJudgeResult(), testResult.getStdout(), testResult.getStderr());
    }


    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "test-queue"),
                    exchange = @Exchange(name = "judge-exchange"),
                    key = "test"
            )
    )
    public void test(JudgeMessage message) {

        TestResult testResult;
        message.setTimeLimit(9999999999999999L);
        message.setMemoryLimit(9999999999999999L);
        message.setStackLimit(999999999);
        try {
            testResult = judgeService.test(message);
        } catch (SystemError | SubmitError e) {
            log.error("判题机出错");
            log.error(e.getMessage(), e);
            return;
        } catch (RuntimeException e) {
            log.error("判题机收到非法语言：{}", e.getMessage());
            return;
        }

        testResult.setUserId(message.getUserId());

        redisJudgeTestUtil.save(testResult, 10);

    }

}
