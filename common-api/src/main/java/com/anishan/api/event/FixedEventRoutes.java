package com.anishan.api.event;

import org.springframework.amqp.core.Message;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Fixed protocol routing allow-list. This class contains no domain service dependencies. */
public final class FixedEventRoutes {

    public static final int MAX_EVENT_PAYLOAD_BYTES = 16 * 1024;

    public static final String COMMUNITY_EXCHANGE = "community.events.v1";
    public static final String COMMUNITY_QUEUE = "user-community-notification.v1";
    public static final String COMMUNITY_DEAD_LETTER_EXCHANGE = "community.events.dlx.v1";
    public static final String COMMUNITY_DEAD_LETTER_QUEUE = "user-community-notification.dlq.v1";
    public static final String COMMUNITY_DEAD_LETTER_ROUTING_KEY = "user-community-notification.dead.v1";

    public static final String POINTS_EXCHANGE = "account.points.v1";
    public static final String POINTS_ROUTING_KEY = "points.awarded.v1";
    public static final String POINTS_QUEUE = "user-point-award.v1";
    public static final String POINTS_DEAD_LETTER_EXCHANGE = "account.points.dlx.v1";
    public static final String POINTS_DEAD_LETTER_QUEUE = "user-point-award.dlq.v1";
    public static final String POINTS_DEAD_LETTER_ROUTING_KEY = "user-point-award.dead.v1";

    private static final Map<CommunityEventType, String> COMMUNITY_ROUTING_KEYS;

    static {
        EnumMap<CommunityEventType, String> routes = new EnumMap<>(CommunityEventType.class);
        routes.put(CommunityEventType.SOLUTION_PUBLISHED, "solution.published.v1");
        routes.put(CommunityEventType.SOLUTION_LIKED, "solution.liked.v1");
        routes.put(CommunityEventType.SOLUTION_COMMENTED, "solution.commented.v1");
        routes.put(CommunityEventType.COMMENT_REPLIED, "comment.replied.v1");
        routes.put(CommunityEventType.COMMENT_LIKED, "comment.liked.v1");
        routes.put(CommunityEventType.SOLUTION_MODERATED, "solution.moderated.v1");
        routes.put(CommunityEventType.COMMENT_MODERATED, "comment.moderated.v1");
        COMMUNITY_ROUTING_KEYS = Collections.unmodifiableMap(routes);
    }

    private FixedEventRoutes() {
    }

    public static String communityRoutingKey(CommunityEventType eventType) {
        String routingKey = COMMUNITY_ROUTING_KEYS.get(eventType);
        if (routingKey == null) {
            throw new IllegalArgumentException("Unsupported community event type");
        }
        return routingKey;
    }

    public static Map<CommunityEventType, String> communityRoutingKeys() {
        return COMMUNITY_ROUTING_KEYS;
    }

    public static void setMessageId(Message message, String eventId) {
        CommunityEvent.validateUuid(eventId);
        message.getMessageProperties().setMessageId(eventId);
    }

    public static void validatePayloadSize(byte[] payload) {
        if (payload == null || payload.length > MAX_EVENT_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Event payload exceeds the 16 KiB limit");
        }
    }

    public static String newEventId() {
        return UUID.randomUUID().toString();
    }
}
