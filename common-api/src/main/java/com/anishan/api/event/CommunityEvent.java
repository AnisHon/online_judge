package com.anishan.api.event;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Versioned, content-free event envelope shared by content-service and user-service.
 * The DTO deliberately has no polymorphic type metadata; callers select this fixed type.
 */
@Getter
@NoArgsConstructor
@Accessors(chain = true)
@Setter
public class CommunityEvent {

    private static final Pattern ID_PATTERN = Pattern.compile("[1-9][0-9]{0,18}");

    @JsonDeserialize(using = StrictIntegerDeserializer.class)
    private Integer schemaVersion;

    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String eventId;

    @JsonDeserialize(using = StrictEventTypeDeserializer.class)
    private CommunityEventType eventType;

    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String dedupeKey;

    /** Kept as a string so the wire format must be an ISO timestamp with an explicit offset. */
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String occurredAt;

    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String actorId;

    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String solutionId;

    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String commentId;

    @JsonDeserialize(contentUsing = StrictStringDeserializer.class)
    private List<String> recipientIds;

    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String action;

    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String reason;

    @JsonIgnore
    private boolean unsupportedProperties;

    @JsonAnySetter
    public void recordUnsupportedProperty(String property, JsonNode value) {
        unsupportedProperties = true;
    }

    /** Validates the stable event contract without logging or echoing payload values. */
    public void validate() {
        if (unsupportedProperties) {
            throw invalid("Unsupported event property");
        }
        if (!Integer.valueOf(1).equals(schemaVersion)) {
            throw invalid("Unsupported community event schema");
        }
        validateUuid(eventId);
        if (eventType == null) {
            throw invalid("Missing community event type");
        }
        validateTimestamp(occurredAt);
        validateId(actorId, "actorId");
        validateId(solutionId, "solutionId");
        if (recipientIds == null || recipientIds.size() > 2) {
            throw invalid("Invalid event recipients");
        }

        Set<String> recipients = new HashSet<>();
        boolean moderationEvent = eventType == CommunityEventType.SOLUTION_MODERATED
                || eventType == CommunityEventType.COMMENT_MODERATED;
        for (String recipientId : recipientIds) {
            validateId(recipientId, "recipientId");
            // Moderation outcomes must still reach the author when an administrator
            // moderates their own content. Self-notifications remain forbidden for
            // ordinary likes/comments and are filtered again by user-service.
            if ((!moderationEvent && recipientId.equals(actorId)) || !recipients.add(recipientId)) {
                throw invalid("Invalid event recipients");
            }
        }

        boolean commentEvent = eventType == CommunityEventType.SOLUTION_COMMENTED
                || eventType == CommunityEventType.COMMENT_REPLIED
                || eventType == CommunityEventType.COMMENT_LIKED
                || eventType == CommunityEventType.COMMENT_MODERATED;
        if (commentEvent) {
            validateId(commentId, "commentId");
        } else if (commentId != null) {
            throw invalid("Unexpected comment id");
        }

        if (eventType == CommunityEventType.SOLUTION_PUBLISHED && !recipientIds.isEmpty()) {
            throw invalid("Published event recipients must be resolved by user-service");
        }

        String expectedDedupeKey = expectedDedupeKey();
        if (dedupeKey == null || !dedupeKey.equals(expectedDedupeKey)) {
            throw invalid("Invalid community event dedupe key");
        }

        validateModerationFields();
    }

    /** Validates fields and the exact serialized JSON byte limit used by the broker contract. */
    public void validate(ObjectMapper objectMapper) {
        try {
            FixedEventRoutes.validatePayloadSize(objectMapper.writeValueAsBytes(this));
        } catch (IOException exception) {
            throw invalid("Unable to serialize community event");
        }
        validate();
    }

    /** Checks the original broker payload size before JSON parsing, then validates its fixed schema. */
    public static CommunityEvent fromJson(byte[] payload, ObjectMapper objectMapper) throws IOException {
        FixedEventRoutes.validatePayloadSize(payload);
        CommunityEvent event = objectMapper.readValue(payload, CommunityEvent.class);
        event.validate(objectMapper);
        return event;
    }

    private String expectedDedupeKey() {
        switch (eventType) {
            case SOLUTION_PUBLISHED:
                return "solution-published:" + solutionId;
            case SOLUTION_LIKED:
                return "solution-liked:" + solutionId + ":" + actorId;
            case SOLUTION_COMMENTED:
                return "solution-commented:" + commentId;
            case COMMENT_REPLIED:
                return "comment-replied:" + commentId;
            case COMMENT_LIKED:
                return "comment-liked:" + commentId + ":" + actorId;
            case SOLUTION_MODERATED:
                return moderationDedupe("solution-moderated:");
            case COMMENT_MODERATED:
                return moderationDedupe("comment-moderated:");
            default:
                throw invalid("Unsupported community event type");
        }
    }

    private String moderationDedupe(String prefix) {
        if (dedupeKey == null || !dedupeKey.startsWith(prefix)) {
            throw invalid("Invalid moderation event dedupe key");
        }
        String actionId = dedupeKey.substring(prefix.length());
        validateId(actionId, "actionId");
        return prefix + actionId;
    }

    private void validateModerationFields() {
        if (eventType == CommunityEventType.SOLUTION_MODERATED) {
            if (!("AUTHOR_ONLY".equals(action) || "RESTORE".equals(action) || "DELETE".equals(action))) {
                throw invalid("Invalid solution moderation action");
            }
            if ("AUTHOR_ONLY".equals(action) || "DELETE".equals(action)) {
                validateReason(reason, true);
            } else if (reason != null) {
                validateReason(reason, false);
            }
            return;
        }
        if (eventType == CommunityEventType.COMMENT_MODERATED) {
            if (!"DELETE".equals(action)) {
                throw invalid("Invalid comment moderation action");
            }
            validateReason(reason, true);
            return;
        }
        if (action != null || reason != null) {
            throw invalid("Unexpected moderation fields");
        }
    }

    private static void validateReason(String value, boolean required) {
        if (value == null) {
            if (required) {
                throw invalid("Moderation reason is required");
            }
            return;
        }
        int codePoints = value.codePointCount(0, value.length());
        if (codePoints < 1 || codePoints > 500 || value.trim().isEmpty()) {
            throw invalid("Invalid moderation reason");
        }
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    throw invalid("Invalid moderation reason");
                }
                index++;
            } else if (Character.isLowSurrogate(current)) {
                throw invalid("Invalid moderation reason");
            } else if (Character.isISOControl(current) && current != '\t' && current != '\n') {
                throw invalid("Invalid moderation reason");
            }
        }
    }

    static void validateTimestamp(String value) {
        if (value == null) {
            throw invalid("Missing event timestamp");
        }
        try {
            OffsetDateTime.parse(value);
        } catch (RuntimeException exception) {
            throw invalid("Invalid event timestamp");
        }
    }

    static void validateId(String value, String fieldName) {
        if (value == null || !ID_PATTERN.matcher(value).matches()) {
            throw invalid("Invalid " + fieldName);
        }
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0 || !Long.toString(parsed).equals(value)) {
                throw invalid("Invalid " + fieldName);
            }
        } catch (NumberFormatException exception) {
            throw invalid("Invalid " + fieldName);
        }
    }

    public static void validateUuid(String value) {
        if (value == null) {
            throw invalid("Missing event id");
        }
        try {
            UUID parsed = UUID.fromString(value);
            if (!parsed.toString().equalsIgnoreCase(value)) {
                throw invalid("Invalid event id");
            }
        } catch (IllegalArgumentException exception) {
            throw invalid("Invalid event id");
        }
    }

    private static IllegalArgumentException invalid(String safeMessage) {
        return new IllegalArgumentException(safeMessage);
    }

    /** Jackson must not coerce numeric IDs or other scalar types into wire strings. */
    public static class StrictStringDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.currentToken() != JsonToken.VALUE_STRING) {
                throw JsonMappingException.from(parser, "Expected JSON string");
            }
            return parser.getText();
        }
    }

    public static class StrictIntegerDeserializer extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
                throw JsonMappingException.from(parser, "Expected JSON integer");
            }
            return parser.getIntValue();
        }
    }

    public static class StrictEventTypeDeserializer extends JsonDeserializer<CommunityEventType> {
        @Override
        public CommunityEventType deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.currentToken() != JsonToken.VALUE_STRING) {
                throw JsonMappingException.from(parser, "Expected JSON event type string");
            }
            try {
                return CommunityEventType.valueOf(parser.getText());
            } catch (IllegalArgumentException exception) {
                throw JsonMappingException.from(parser, "Unsupported community event type");
            }
        }
    }
}
