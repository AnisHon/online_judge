package com.anishan.user.mq;

import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.api.event.FixedEventRoutes;
import com.anishan.user.service.CommunityEventFreshness;
import com.anishan.user.service.NotificationDeliveryService;
import com.anishan.user.service.NotificationFanoutService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Receives raw bytes so Rabbit type headers can never select arbitrary Java classes. */
@Component
@RequiredArgsConstructor
public class CommunityNotificationListener {

    private final ObjectMapper objectMapper;
    private final CommunityEventFreshness freshness;
    private final NotificationDeliveryService deliveryService;
    private final NotificationFanoutService fanoutService;

    @RabbitListener(queues = FixedEventRoutes.COMMUNITY_QUEUE,
            containerFactory = "communityListenerContainerFactory")
    public void onMessage(Message message) {
        CommunityEvent event = decode(message);
        if (freshness.isExpired(event)) {
            freshness.recordExpired(event.getEventId(), event.getEventType().name());
            return;
        }

        if (event.getEventType() == CommunityEventType.SOLUTION_PUBLISHED) {
            // Registering the durable cursor is enough to ACK; the listener never walks followers.
            fanoutService.register(event);
            return;
        }
        deliveryService.deliver(event);
    }

    private CommunityEvent decode(Message message) {
        try {
            if (message == null || message.getBody() == null) {
                throw new IOException("Missing message body");
            }
            CommunityEvent event = CommunityEvent.fromJson(message.getBody(), objectMapper);
            FixedEventRoutes.validatePayloadSize(message.getBody());
            String messageId = message.getMessageProperties().getMessageId();
            if (!event.getEventId().equals(messageId)) {
                throw new IOException("Message id does not match event id");
            }
            return event;
        } catch (IOException | RuntimeException invalid) {
            // Do not attach the parser exception: it may contain event reason or raw payload data.
            throw new AmqpRejectAndDontRequeueException("Invalid community event envelope");
        }
    }
}
