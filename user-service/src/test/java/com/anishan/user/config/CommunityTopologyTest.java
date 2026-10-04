package com.anishan.user.config;

import com.anishan.api.event.FixedEventRoutes;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommunityTopologyTest {

    @Test
    void declaresDurableCommunityPointsAndDeadLetterTopologyWithoutJudgeQueueChanges() {
        Declarables declarables = new CommunityTopologyConfig().communityEventTopology();
        Collection<Declarable> all = declarables.getDeclarables();
        Map<String, Exchange> exchanges = all.stream()
                .filter(Exchange.class::isInstance)
                .map(Exchange.class::cast)
                .collect(Collectors.toMap(Exchange::getName, Function.identity()));
        Map<String, Queue> queues = all.stream()
                .filter(Queue.class::isInstance)
                .map(Queue.class::cast)
                .collect(Collectors.toMap(Queue::getName, Function.identity()));
        List<Binding> bindings = all.stream()
                .filter(Binding.class::isInstance)
                .map(Binding.class::cast)
                .collect(Collectors.toList());

        assertExchange(exchanges, FixedEventRoutes.COMMUNITY_EXCHANGE, "topic");
        assertExchange(exchanges, FixedEventRoutes.COMMUNITY_DEAD_LETTER_EXCHANGE, "direct");
        assertExchange(exchanges, FixedEventRoutes.POINTS_EXCHANGE, "direct");
        assertExchange(exchanges, FixedEventRoutes.POINTS_DEAD_LETTER_EXCHANGE, "direct");

        assertQueue(queues, FixedEventRoutes.COMMUNITY_QUEUE,
                FixedEventRoutes.COMMUNITY_DEAD_LETTER_EXCHANGE,
                FixedEventRoutes.COMMUNITY_DEAD_LETTER_ROUTING_KEY);
        assertQueue(queues, FixedEventRoutes.POINTS_QUEUE,
                FixedEventRoutes.POINTS_DEAD_LETTER_EXCHANGE,
                FixedEventRoutes.POINTS_DEAD_LETTER_ROUTING_KEY);
        assertDurableQueue(queues, FixedEventRoutes.COMMUNITY_DEAD_LETTER_QUEUE);
        assertDurableQueue(queues, FixedEventRoutes.POINTS_DEAD_LETTER_QUEUE);

        assertBinding(bindings, FixedEventRoutes.COMMUNITY_QUEUE,
                FixedEventRoutes.COMMUNITY_EXCHANGE, "solution.*.v1");
        assertBinding(bindings, FixedEventRoutes.COMMUNITY_QUEUE,
                FixedEventRoutes.COMMUNITY_EXCHANGE, "comment.*.v1");
        FixedEventRoutes.communityRoutingKeys().values().forEach(routingKey -> {
            String bindingKey = routingKey.startsWith("solution.") ? "solution.*.v1" : "comment.*.v1";
            assertBinding(bindings, FixedEventRoutes.COMMUNITY_QUEUE,
                    FixedEventRoutes.COMMUNITY_EXCHANGE, bindingKey);
        });
        assertBinding(bindings, FixedEventRoutes.COMMUNITY_DEAD_LETTER_QUEUE,
                FixedEventRoutes.COMMUNITY_DEAD_LETTER_EXCHANGE,
                FixedEventRoutes.COMMUNITY_DEAD_LETTER_ROUTING_KEY);
        assertBinding(bindings, FixedEventRoutes.POINTS_QUEUE,
                FixedEventRoutes.POINTS_EXCHANGE, FixedEventRoutes.POINTS_ROUTING_KEY);
        assertBinding(bindings, FixedEventRoutes.POINTS_DEAD_LETTER_QUEUE,
                FixedEventRoutes.POINTS_DEAD_LETTER_EXCHANGE,
                FixedEventRoutes.POINTS_DEAD_LETTER_ROUTING_KEY);

        assertFalse(queues.containsKey("judge-info-queue"));
        assertFalse(exchanges.containsKey("judge-exchange"));
    }

    private void assertExchange(Map<String, Exchange> exchanges, String name, String type) {
        Exchange exchange = exchanges.get(name);
        assertNotNull(exchange, name);
        assertEquals(type, exchange.getType());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
    }

    private void assertQueue(Map<String, Queue> queues, String name, String deadLetterExchange,
                             String deadLetterRoutingKey) {
        Queue queue = assertDurableQueue(queues, name);
        assertEquals(deadLetterExchange, queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals(deadLetterRoutingKey, queue.getArguments().get("x-dead-letter-routing-key"));
    }

    private Queue assertDurableQueue(Map<String, Queue> queues, String name) {
        Queue queue = queues.get(name);
        assertNotNull(queue, name);
        assertTrue(queue.isDurable());
        assertFalse(queue.isExclusive());
        assertFalse(queue.isAutoDelete());
        return queue;
    }

    private void assertBinding(List<Binding> bindings, String destination, String exchange, String routingKey) {
        assertTrue(bindings.stream().anyMatch(binding -> destination.equals(binding.getDestination())
                && exchange.equals(binding.getExchange()) && routingKey.equals(binding.getRoutingKey())),
                destination + " <- " + exchange + " / " + routingKey);
    }
}
