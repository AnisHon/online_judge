package com.anishan.content.job;

import com.anishan.content.config.CommunityWorkerProperties;
import com.anishan.content.service.ContentEventOutboxService;
import com.anishan.content.service.SolutionDomainMigrationGate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "oj.content-community-outbox", name = "enabled", havingValue = "true")
public class ContentOutboxCleanupJob {

    private final ContentEventOutboxService outboxService;
    private final SolutionDomainMigrationGate migrationGate;

    public ContentOutboxCleanupJob(ContentEventOutboxService outboxService,
                                   SolutionDomainMigrationGate migrationGate) {
        this.outboxService = outboxService;
        this.migrationGate = migrationGate;
    }

    @Scheduled(fixedDelayString = "${oj.content-community-outbox.cleanup-interval-ms:3600000}",
            initialDelayString = "${oj.content-community-outbox.cleanup-interval-ms:3600000}")
    public void cleanup() {
        try {
            if (!migrationGate.isCutover()) {
                return;
            }
            int deleted = outboxService.cleanupSentBatch();
            if (deleted > 0) {
                log.info("Content outbox cleanup completed deleted={}", deleted);
            }
        } catch (RuntimeException failure) {
            log.error("Content outbox cleanup failed errorType={}", failure.getClass().getSimpleName());
        }
    }
}
