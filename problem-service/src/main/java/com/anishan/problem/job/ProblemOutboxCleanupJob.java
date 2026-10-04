package com.anishan.problem.job;

import com.anishan.problem.service.ProblemEventOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "oj.problem-outbox", name = "enabled", havingValue = "true")
public class ProblemOutboxCleanupJob {

    private final ProblemEventOutboxService outboxService;

    @Scheduled(fixedDelayString = "${oj.problem-outbox.cleanup-interval-ms:3600000}")
    public void cleanupSentBatch() {
        try {
            int deleted = outboxService.cleanupSentBatch();
            if (deleted > 0) {
                log.info("Problem outbox cleanup removed sentRows={}", deleted);
            }
        } catch (RuntimeException failure) {
            log.error("Problem outbox cleanup failed errorType={}", failure.getClass().getSimpleName());
        }
    }
}
