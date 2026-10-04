package com.anishan.content.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;
import java.time.ZoneId;

/** Dedicated publisher resources; exchanges and queues are owned by user-service. */
@Configuration
@EnableConfigurationProperties(CommunityWorkerProperties.class)
public class CommunityMessagingConfig {

    @Bean(name = "contentCommunityRabbitTemplate")
    public RabbitTemplate contentCommunityRabbitTemplate(ConnectionFactory connectionFactory,
                                                          MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        // In Spring AMQP 2.4, mandatory publishing requires a ReturnsCallback. The shared
        // CachingConnectionFactory attaches a returned message to the CorrelationData.
        template.setReturnsCallback(returned -> { });
        return template;
    }

    @Bean(name = "contentCommunityPublisherExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor contentCommunityPublisherExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("content-community-outbox-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }

    @Bean(name = "contentCommunityOutboxClock")
    public Clock contentCommunityOutboxClock() {
        return Clock.system(ZoneId.of("Asia/Shanghai"));
    }
}
