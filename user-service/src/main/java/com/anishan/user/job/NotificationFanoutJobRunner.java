package com.anishan.user.job;

import com.anishan.user.domain.entity.NotificationFanoutJob;
import com.anishan.user.service.NotificationFanoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Handles one bounded follower batch per scheduled cycle; no per-job threads are created. */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "oj.notification-worker", name = "community-enabled", havingValue = "true")
public class NotificationFanoutJobRunner {

    private final NotificationFanoutService fanoutService;
    private final String leaseOwner = UUID.randomUUID().toString();
    private final AtomicBoolean cycleActive = new AtomicBoolean(false);

    public NotificationFanoutJobRunner(NotificationFanoutService fanoutService) {
        this.fanoutService = fanoutService;
    }

    @Scheduled(fixedDelayString = "${oj.notification-worker.fanout-poll-interval-ms:1000}")
    public void runOneBatch() {
        if (!cycleActive.compareAndSet(false, true)) {
            return;
        }
        try {
            NotificationFanoutJob job = fanoutService.claimOne(leaseOwner);
            if (job == null) {
                return;
            }
            try {
                fanoutService.processOneBatch(job);
            } catch (RuntimeException failure) {
                log.error("Notification fanout batch failed eventId={} stage=PROCESS errorType={}",
                        job.getEventId(), failure.getClass().getSimpleName());
                try {
                    fanoutService.markFailed(job, "FANOUT_FAILURE");
                } catch (RuntimeException updateFailure) {
                    log.error("Notification fanout failure update failed eventId={} stage=MARK_FAILED errorType={}",
                            job.getEventId(), updateFailure.getClass().getSimpleName());
                }
            }
        } catch (RuntimeException failure) {
            log.error("Notification fanout poll failed stage=CLAIM errorType={}",
                    failure.getClass().getSimpleName());
        } finally {
            cycleActive.set(false);
        }
    }
}
