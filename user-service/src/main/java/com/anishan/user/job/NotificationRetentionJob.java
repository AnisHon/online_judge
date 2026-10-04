package com.anishan.user.job;

import com.anishan.user.service.NotificationQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Deletes expired data in independent transactions of at most 500 rows each. */
@Component
@Slf4j
public class NotificationRetentionJob {

    private static final int BATCH_SIZE = 500;

    private final NotificationQueryService notificationQueryService;
    private final Clock clock;

    public NotificationRetentionJob(NotificationQueryService notificationQueryService,
                                    @Qualifier("userCommunityClock") Clock clock) {
        this.notificationQueryService = notificationQueryService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "Asia/Shanghai")
    public void cleanupExpiredRows() {
        long notifications = drainBatches(notificationQueryService::deleteExpiredNotificationsBatch);
        long fanoutJobs = drainBatches(notificationQueryService::deleteCompletedFanoutBatch);
        if (notifications > 0 || fanoutJobs > 0) {
            log.info("Notification retention cleanup completed notifications={} doneFanoutJobs={} at={}",
                    notifications, fanoutJobs, clock.instant());
        }
    }

    private static long drainBatches(java.util.function.IntSupplier deleteBatch) {
        long total = 0L;
        int deleted;
        do {
            deleted = deleteBatch.getAsInt();
            total += deleted;
        } while (deleted == BATCH_SIZE);
        return total;
    }
}
