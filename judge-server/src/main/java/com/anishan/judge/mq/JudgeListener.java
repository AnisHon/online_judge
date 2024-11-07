package com.anishan.judge.mq;

import com.anishan.api.client.judgeserver.domain.JudgeMessage;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.problem.client.RecordClient;
import com.anishan.api.client.problem.client.SubmitLogClient;
import com.anishan.api.client.problem.domain.dto.SubmitLogDto;
import com.anishan.commons.e.JudgeResult;
import com.anishan.judge.exception.SubmitError;
import com.anishan.judge.exception.SystemError;
import com.anishan.judge.service.JudgeService;
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

    private final JudgeService judgeService;
    private final SubmitLogClient submitLogClient;
    private final RecordClient recordClient;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "judge-queue"),
                    exchange = @Exchange(name = "judge-exchange"),
                    key = "judge"
            ),
            concurrency = "2-4"
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
                        .setStderr(judgeScore.getErrorMessage());



        // 更新日志状态
        submitLogClient.update(submitLogDto, message.getUserId());

        // 更新record
        recordClient.judgeSave(judgeScore, message.getUserId());



    }


}
