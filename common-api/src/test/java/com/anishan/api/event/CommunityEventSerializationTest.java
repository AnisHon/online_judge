package com.anishan.api.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommunityEventSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void communityEventRoundTripsWithStringIdsAndFixedEventType() throws Exception {
        CommunityEvent event = publishedEvent();
        byte[] json = objectMapper.writeValueAsBytes(event);
        CommunityEvent restored = objectMapper.readValue(json, CommunityEvent.class);

        restored.validate(objectMapper);
        assertEquals("9007199254740993123", restored.getSolutionId());
        assertEquals(CommunityEventType.SOLUTION_PUBLISHED, restored.getEventType());
        assertTrue(objectMapper.readTree(json).get("solutionId").isTextual());
    }

    @Test
    void rejectsUnknownSchemaUnknownFieldsAndNonStringWireValues() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            CommunityEvent event = publishedEvent().setSchemaVersion(2);
            event.validate();
        });
        assertThrows(Exception.class, () -> objectMapper.readValue(
                communityJson().replace("\"actorId\":\"7\"", "\"actorId\":7"), CommunityEvent.class));
        assertThrows(Exception.class, () -> objectMapper.readValue(
                communityJson().replace("\"eventType\":\"SOLUTION_PUBLISHED\"", "\"eventType\":99"),
                CommunityEvent.class));
        assertThrows(Exception.class, () -> objectMapper.readValue(
                communityJson().replace("SOLUTION_PUBLISHED", "SOLUTION_REMOVED"), CommunityEvent.class));

        CommunityEvent withUnknownField = readCommunity(communityJson().replace(
                "\"reason\":null", "\"reason\":null,\"code\":\"sensitive\""));
        assertThrows(IllegalArgumentException.class, withUnknownField::validate);
    }

    @Test
    void rejectsInvalidRecipientsReasonDedupeAndOversizedPayload() {
        CommunityEvent tooManyRecipients = publishedEvent().setRecipientIds(Arrays.asList("8", "9", "10"));
        assertThrows(IllegalArgumentException.class, tooManyRecipients::validate);

        CommunityEvent duplicatedRecipient = publishedEvent()
                .setEventType(CommunityEventType.SOLUTION_LIKED)
                .setDedupeKey("solution-liked:9007199254740993123:7")
                .setRecipientIds(Collections.singletonList("7"));
        assertThrows(IllegalArgumentException.class, duplicatedRecipient::validate);

        CommunityEvent invalidDedupe = publishedEvent().setDedupeKey("arbitrary-route:1");
        assertThrows(IllegalArgumentException.class, invalidDedupe::validate);

        CommunityEvent invalidReason = publishedEvent()
                .setEventType(CommunityEventType.COMMENT_MODERATED)
                .setCommentId("100")
                .setAction("DELETE")
                .setReason("x".repeat(501))
                .setDedupeKey("comment-moderated:101");
        assertThrows(IllegalArgumentException.class, invalidReason::validate);

        CommunityEvent controlCharacterReason = moderatedComment("removed\u0000comment");
        assertThrows(IllegalArgumentException.class, controlCharacterReason::validate);
        moderatedComment("<script>plain text</script>").validate();

        assertThrows(IllegalArgumentException.class,
                () -> FixedEventRoutes.validatePayloadSize(new byte[FixedEventRoutes.MAX_EVENT_PAYLOAD_BYTES + 1]));
        assertThrows(IllegalArgumentException.class, () -> CommunityEvent.fromJson(
                new byte[FixedEventRoutes.MAX_EVENT_PAYLOAD_BYTES + 1], objectMapper));
    }

    @Test
    void moderationMayNotifyItsActorButOrdinarySelfNotificationsRemainInvalid() {
        CommunityEvent moderation = moderatedComment("需要调整")
                .setRecipientIds(Collections.singletonList("7"));
        moderation.validate();

        CommunityEvent selfLike = publishedEvent()
                .setEventType(CommunityEventType.SOLUTION_LIKED)
                .setDedupeKey("solution-liked:9007199254740993123:7")
                .setRecipientIds(Collections.singletonList("7"));
        assertThrows(IllegalArgumentException.class, selfLike::validate);
    }

    @Test
    void pointsEventPreservesExactDecimalAndRejectsNumericJsonAmount() throws Exception {
        String eventId = UUID.randomUUID().toString();
        PointAwardEvent event = new PointAwardEvent()
                .setSchemaVersion(1)
                .setEventId(eventId)
                .setDedupeKey("points-awarded:100:200")
                .setOccurredAt("2026-10-03T12:00:00.123+08:00")
                .setUserId("100")
                .setProblemId("200")
                .setAmount("0.123456789012345678901");

        PointAwardEvent restored = objectMapper.readValue(objectMapper.writeValueAsBytes(event), PointAwardEvent.class);
        restored.validate(objectMapper);
        assertEquals("0.123456789012345678901", restored.getAmount());

        String numericAmount = "{\"schemaVersion\":1,\"eventId\":\"" + eventId
                + "\",\"dedupeKey\":\"points-awarded:100:200\","
                + "\"occurredAt\":\"2026-10-03T12:00:00+08:00\",\"userId\":\"100\","
                + "\"problemId\":\"200\",\"amount\":1.25}";
        assertThrows(Exception.class, () -> objectMapper.readValue(numericAmount, PointAwardEvent.class));
    }

    @Test
    void fixedRouteMappingIsClosedAndMessageIdUsesEventId() {
        Map<CommunityEventType, String> expected = new EnumMap<>(CommunityEventType.class);
        expected.put(CommunityEventType.SOLUTION_PUBLISHED, "solution.published.v1");
        expected.put(CommunityEventType.SOLUTION_LIKED, "solution.liked.v1");
        expected.put(CommunityEventType.SOLUTION_COMMENTED, "solution.commented.v1");
        expected.put(CommunityEventType.COMMENT_REPLIED, "comment.replied.v1");
        expected.put(CommunityEventType.COMMENT_LIKED, "comment.liked.v1");
        expected.put(CommunityEventType.SOLUTION_MODERATED, "solution.moderated.v1");
        expected.put(CommunityEventType.COMMENT_MODERATED, "comment.moderated.v1");
        assertEquals(expected, FixedEventRoutes.communityRoutingKeys());

        String eventId = UUID.randomUUID().toString();
        Message message = new Message(new byte[0], new MessageProperties());
        FixedEventRoutes.setMessageId(message, eventId);
        assertEquals(eventId, message.getMessageProperties().getMessageId());
    }

    private CommunityEvent publishedEvent() {
        return new CommunityEvent()
                .setSchemaVersion(1)
                .setEventId(UUID.fromString("e1a9a8b3-4335-4fcb-9f74-8c42c361c44b").toString())
                .setEventType(CommunityEventType.SOLUTION_PUBLISHED)
                .setDedupeKey("solution-published:9007199254740993123")
                .setOccurredAt("2026-10-03T12:00:00.123+08:00")
                .setActorId("7")
                .setSolutionId("9007199254740993123")
                .setRecipientIds(Collections.emptyList());
    }

    private String communityJson() throws Exception {
        return objectMapper.writeValueAsString(publishedEvent().setReason(null).setAction(null));
    }

    private CommunityEvent moderatedComment(String reason) {
        return publishedEvent()
                .setEventType(CommunityEventType.COMMENT_MODERATED)
                .setCommentId("100")
                .setRecipientIds(Collections.singletonList("8"))
                .setDedupeKey("comment-moderated:101")
                .setAction("DELETE")
                .setReason(reason);
    }

    private CommunityEvent readCommunity(String json) {
        try {
            return objectMapper.readValue(json, CommunityEvent.class);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
