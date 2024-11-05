package com.anishan.judge.mq;

import com.anishan.api.client.judgeserver.domain.JudgeMessage;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.judge.exception.CompileError;
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

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "judge-queue"),
                    exchange = @Exchange(name = "judge-exchange"),
                    key = "judge"
            ),
            concurrency = "2-4"
    )
    public void judge(JudgeMessage message) {

        JudgeScore judgeScore = null;
        try {
            judgeScore = judgeService.judge(message);
        } catch (SystemError | SubmitError e) {
           log.error("判题机出错");
           log.error(e.getMessage(), e);
        }


    }

}
