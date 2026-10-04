package com.anishan.user.mq;

import com.anishan.api.event.FixedEventRoutes;
import com.anishan.api.event.PointAwardEvent;
import com.anishan.user.service.PointAwardApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Point delivery is independent of community notification retention and dedupe. */
@Component
@RequiredArgsConstructor
public class PointAwardListener {

    private final ObjectMapper objectMapper;
    private final PointAwardApplicationService pointAwardService;

    @RabbitListener(queues = FixedEventRoutes.POINTS_QUEUE,
            containerFactory = "pointAwardListenerContainerFactory")
    public void onMessage(Message message) {
        PointAwardEvent event = decode(message);
        pointAwardService.apply(event);
    }

    private PointAwardEvent decode(Message message) {
        try {
            if (message == null || message.getBody() == null) {
                throw new IOException("Missing message body");
            }
            PointAwardEvent event = PointAwardEvent.fromJson(message.getBody(), objectMapper);
            String messageId = message.getMessageProperties().getMessageId();
            if (!event.getEventId().equals(messageId)) {
                throw new IOException("Message id does not match event id");
            }
            return event;
        } catch (IOException | RuntimeException invalid) {
            throw new AmqpRejectAndDontRequeueException("Invalid points event envelope");
        }
    }
}
