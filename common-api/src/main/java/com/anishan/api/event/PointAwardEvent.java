package com.anishan.api.event;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.math.BigDecimal;

/** Internal, exact-decimal event sent only through the fixed account-points route. */
@Getter
@NoArgsConstructor
@Accessors(chain = true)
@Setter
public class PointAwardEvent {

    @JsonDeserialize(using = CommunityEvent.StrictIntegerDeserializer.class)
    private Integer schemaVersion;

    @JsonDeserialize(using = CommunityEvent.StrictStringDeserializer.class)
    private String eventId;

    @JsonDeserialize(using = CommunityEvent.StrictStringDeserializer.class)
    private String dedupeKey;

    @JsonDeserialize(using = CommunityEvent.StrictStringDeserializer.class)
    private String occurredAt;

    @JsonDeserialize(using = CommunityEvent.StrictStringDeserializer.class)
    private String userId;

    @JsonDeserialize(using = CommunityEvent.StrictStringDeserializer.class)
    private String problemId;

    @JsonDeserialize(using = CommunityEvent.StrictStringDeserializer.class)
    private String amount;

    @JsonIgnore
    private boolean unsupportedProperties;

    @JsonAnySetter
    public void recordUnsupportedProperty(String property, JsonNode value) {
        unsupportedProperties = true;
    }

    public void validate() {
        if (unsupportedProperties) {
            throw invalid("Unsupported points event property");
        }
        if (!Integer.valueOf(1).equals(schemaVersion)) {
            throw invalid("Unsupported points event schema");
        }
        CommunityEvent.validateUuid(eventId);
        CommunityEvent.validateId(userId, "userId");
        CommunityEvent.validateId(problemId, "problemId");
        CommunityEvent.validateTimestamp(occurredAt);
        if (amount == null || !amount.matches("(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?")) {
            throw invalid("Invalid points amount");
        }
        try {
            if (new BigDecimal(amount).signum() <= 0) {
                throw invalid("Invalid points amount");
            }
        } catch (NumberFormatException exception) {
            throw invalid("Invalid points amount");
        }
        String expectedDedupeKey = "points-awarded:" + userId + ":" + problemId;
        if (!expectedDedupeKey.equals(dedupeKey)) {
            throw invalid("Invalid points event dedupe key");
        }
    }

    public void validate(ObjectMapper objectMapper) {
        try {
            FixedEventRoutes.validatePayloadSize(objectMapper.writeValueAsBytes(this));
        } catch (IOException exception) {
            throw invalid("Unable to serialize points event");
        }
        validate();
    }

    public static PointAwardEvent fromJson(byte[] payload, ObjectMapper objectMapper) throws IOException {
        FixedEventRoutes.validatePayloadSize(payload);
        PointAwardEvent event = objectMapper.readValue(payload, PointAwardEvent.class);
        event.validate(objectMapper);
        return event;
    }

    private static IllegalArgumentException invalid(String safeMessage) {
        return new IllegalArgumentException(safeMessage);
    }
}
