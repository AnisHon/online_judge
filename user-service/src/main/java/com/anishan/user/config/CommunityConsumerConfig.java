package com.anishan.user.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Dedicated consumers leave the existing/default listener factory untouched. */
@Configuration
@EnableConfigurationProperties(NotificationWorkerProperties.class)
public class CommunityConsumerConfig {

    @Bean(name = "communityListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory communityListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            NotificationWorkerProperties properties) {
        return listenerFactory(connectionFactory, messageConverter, properties.isCommunityEnabled());
    }

    @Bean(name = "pointAwardListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory pointAwardListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            NotificationWorkerProperties properties) {
        return listenerFactory(connectionFactory, messageConverter, properties.isPointsEnabled());
    }

    @Bean(name = "userCommunityClock")
    public Clock userCommunityClock() {
        return Clock.system(ZoneId.of("Asia/Shanghai"));
    }

    private SimpleRabbitListenerContainerFactory listenerFactory(ConnectionFactory connectionFactory,
                                                                  MessageConverter messageConverter,
                                                                  boolean enabled) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(false);
        factory.setAutoStartup(enabled);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setPrefetchCount(1);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000L, 2.0, 5000L)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return factory;
    }
}
