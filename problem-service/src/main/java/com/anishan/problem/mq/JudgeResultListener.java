package com.anishan.problem.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


//todo 未完成，未使用，待实现
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class JudgeResultListener {

    /**
     *  状态队列，使用一个广播交换机，这里用于保存到数据库,更新SEO
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "judge-status-queue"),
                    exchange = @Exchange(name = "judge-exchange"),
                    key = "status"
            )
    )
    public void changeStatus() {
        // 更新数据库
        // 更新SEO
    }




}
