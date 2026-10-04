package com.anishan.user.config;

import com.anishan.api.event.FixedEventRoutes;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Declares only the durable event topology owned by user-service. */
@Configuration
public class CommunityTopologyConfig {

    @Bean
    public Declarables communityEventTopology() {
        TopicExchange communityExchange = new TopicExchange(
                FixedEventRoutes.COMMUNITY_EXCHANGE, true, false);
        Queue communityQueue = QueueBuilder.durable(FixedEventRoutes.COMMUNITY_QUEUE)
                .deadLetterExchange(FixedEventRoutes.COMMUNITY_DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(FixedEventRoutes.COMMUNITY_DEAD_LETTER_ROUTING_KEY)
                .build();
        DirectExchange communityDeadLetterExchange = new DirectExchange(
                FixedEventRoutes.COMMUNITY_DEAD_LETTER_EXCHANGE, true, false);
        Queue communityDeadLetterQueue = QueueBuilder.durable(
                FixedEventRoutes.COMMUNITY_DEAD_LETTER_QUEUE).build();

        DirectExchange pointsExchange = new DirectExchange(FixedEventRoutes.POINTS_EXCHANGE, true, false);
        Queue pointsQueue = QueueBuilder.durable(FixedEventRoutes.POINTS_QUEUE)
                .deadLetterExchange(FixedEventRoutes.POINTS_DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(FixedEventRoutes.POINTS_DEAD_LETTER_ROUTING_KEY)
                .build();
        DirectExchange pointsDeadLetterExchange = new DirectExchange(
                FixedEventRoutes.POINTS_DEAD_LETTER_EXCHANGE, true, false);
        Queue pointsDeadLetterQueue = QueueBuilder.durable(FixedEventRoutes.POINTS_DEAD_LETTER_QUEUE).build();

        Binding communitySolutionBinding = BindingBuilder.bind(communityQueue)
                .to(communityExchange).with("solution.*.v1");
        Binding communityCommentBinding = BindingBuilder.bind(communityQueue)
                .to(communityExchange).with("comment.*.v1");
        Binding communityDeadLetterBinding = BindingBuilder.bind(communityDeadLetterQueue)
                .to(communityDeadLetterExchange).with(FixedEventRoutes.COMMUNITY_DEAD_LETTER_ROUTING_KEY);
        Binding pointsBinding = BindingBuilder.bind(pointsQueue)
                .to(pointsExchange).with(FixedEventRoutes.POINTS_ROUTING_KEY);
        Binding pointsDeadLetterBinding = BindingBuilder.bind(pointsDeadLetterQueue)
                .to(pointsDeadLetterExchange).with(FixedEventRoutes.POINTS_DEAD_LETTER_ROUTING_KEY);

        return new Declarables(
                communityExchange,
                communityQueue,
                communityDeadLetterExchange,
                communityDeadLetterQueue,
                pointsExchange,
                pointsQueue,
                pointsDeadLetterExchange,
                pointsDeadLetterQueue,
                communitySolutionBinding,
                communityCommentBinding,
                communityDeadLetterBinding,
                pointsBinding,
                pointsDeadLetterBinding);
    }
}
