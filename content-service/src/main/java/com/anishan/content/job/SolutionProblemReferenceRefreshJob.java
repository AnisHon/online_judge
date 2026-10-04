package com.anishan.content.job;

import com.anishan.content.config.SolutionDomainProperties;
import com.anishan.content.domain.entity.SolutionProblemReference;
import com.anishan.content.mapper.SolutionProblemReferenceMapper;
import com.anishan.content.service.SolutionDomainMigrationGate;
import com.anishan.content.service.SolutionProblemReferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SolutionProblemReferenceRefreshJob {
    private static final int BATCH_SIZE = 100;
    private static final ZoneId OJ_ZONE = ZoneId.of("Asia/Shanghai");

    private final SolutionDomainProperties properties;
    private final SolutionDomainMigrationGate migrationGate;
    private final SolutionProblemReferenceMapper referenceMapper;
    private final SolutionProblemReferenceService referenceService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile LocalDateTime cursorCheckedAt;
    private volatile Long cursorProblemId;
    private volatile long nextRetryAt;
    private volatile int failures;

    @Scheduled(fixedDelayString = "${oj.content.solution.reference-refresh-interval-ms:1000}")
    public void refreshBatch() {
        if (!properties.isReferenceRefreshEnabled() || !running.compareAndSet(false, true)) return;
        try {
            if (System.currentTimeMillis() < nextRetryAt || !migrationGate.isCutover()) return;
            LocalDateTime cutoff = LocalDateTime.now(OJ_ZONE).minusMinutes(5);
            List<SolutionProblemReference> candidates = referenceMapper.selectRefreshCandidates(
                    cutoff, cursorCheckedAt, cursorProblemId, BATCH_SIZE);
            if (candidates.isEmpty() && cursorCheckedAt != null) {
                cursorCheckedAt = null;
                cursorProblemId = null;
                candidates = referenceMapper.selectRefreshCandidates(cutoff, null, null, BATCH_SIZE);
            }
            if (candidates.isEmpty()) {
                failures = 0;
                nextRetryAt = 0;
                return;
            }
            referenceService.refresh(candidates.stream().map(SolutionProblemReference::getProblemId)
                    .collect(Collectors.toList()));
            SolutionProblemReference last = candidates.get(candidates.size() - 1);
            cursorCheckedAt = last.getCheckedAt();
            cursorProblemId = last.getProblemId();
            failures = 0;
            nextRetryAt = 0;
        } catch (RuntimeException exception) {
            failures = Math.min(failures + 1, 7);
            long delaySeconds = Math.min(300L, 5L << Math.min(failures - 1, 6));
            nextRetryAt = System.currentTimeMillis() + delaySeconds * 1000L;
            log.warn("Solution problem-reference refresh failed; retrying after {} seconds", delaySeconds);
        } finally {
            running.set(false);
        }
    }
}
