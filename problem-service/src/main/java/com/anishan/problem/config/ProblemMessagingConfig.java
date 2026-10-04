package com.anishan.problem.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableConfigurationProperties(ProblemOutboxProperties.class)
public class ProblemMessagingConfig {

    public static final String JUDGE_EXCHANGE = "judge-exchange";
    public static final String JUDGE_ROUTING_KEY = "judge-info";
    public static final String JUDGE_DEAD_LETTER_EXCHANGE = "judge-exchange.dlx.v1";
    public static final String JUDGE_DEAD_LETTER_QUEUE = "judge-info.dlq.v1";
    public static final String JUDGE_DEAD_LETTER_ROUTING_KEY = "judge-info.dead.v1";

    @Bean(name = "problemOutboxRabbitTemplate")
    public RabbitTemplate problemOutboxRabbitTemplate(ConnectionFactory connectionFactory,
                                                       MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        // In Spring AMQP 2.4 a ReturnsCallback is required for mandatory publishing to take effect.
        // The returned message itself is attached to CorrelationData by the shared CachingConnectionFactory.
        template.setReturnsCallback(returned -> { });
        // Correlated confirms/returns come from the existing shared Rabbit configuration.
        // Do not install callbacks on or replace the global RabbitTemplate.
        return template;
    }

    @Bean(name = "problemOutboxPublisherExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor problemOutboxPublisherExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("problem-outbox-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    @Bean(name = "problemOutboxClock")
    public Clock problemOutboxClock() {
        return Clock.system(ZoneId.of("Asia/Shanghai"));
    }

    @Bean
    public Declarables judgeDispatchDeadLetterTopology() {
        DirectExchange deadLetterExchange = new DirectExchange(JUDGE_DEAD_LETTER_EXCHANGE, true, false);
        Queue deadLetterQueue = QueueBuilder.durable(JUDGE_DEAD_LETTER_QUEUE).build();
        Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange).with(JUDGE_DEAD_LETTER_ROUTING_KEY);
        return new Declarables(deadLetterExchange, deadLetterQueue, deadLetterBinding);
    }
}
