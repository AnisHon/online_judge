package com.anishan.user.service;

import com.anishan.api.event.CommunityEvent;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/** Shared 180-day replay guard for community delivery and persisted fanout work. */
@Component
@Slf4j
public class CommunityEventFreshness {

    private static final Duration RETENTION = Duration.ofDays(180);
    private final Clock clock;

    public CommunityEventFreshness(@Qualifier("userCommunityClock") Clock clock) {
        this.clock = clock;
    }

    public boolean isExpired(CommunityEvent event) {
        OffsetDateTime occurredAt = OffsetDateTime.parse(event.getOccurredAt());
        return occurredAt.toInstant().isBefore(clock.instant().minus(RETENTION));
    }

    public boolean isExpired(LocalDateTime occurredAt) {
        return occurredAt.isBefore(LocalDateTime.now(clock).minusDays(180));
    }

    public void recordExpired(String eventId, String eventType) {
        Metrics.counter("oj.community.events.discarded", "reason", "older_than_180_days",
                "event_type", eventType == null ? "unknown" : eventType).increment();
        log.warn("Community event discarded eventId={} eventType={} reason=OLDER_THAN_180_DAYS",
                eventId, eventType);
    }
}
