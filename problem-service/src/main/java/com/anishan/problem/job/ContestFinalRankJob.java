package com.anishan.problem.job;

import com.anishan.problem.config.ContestRankProperties;
import com.anishan.problem.domain.ContestRankBuildLease;
import com.anishan.problem.service.ContestFinalRankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Bounded in-process generator; no separate service, per-contest thread, or Redis lock. */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "oj.contest-rank", name = "enabled", havingValue = "true")
public class ContestFinalRankJob {

    private static final int BUILD_THREADS = 2;
    private static final int BUILD_QUEUE_CAPACITY = 2;
    private static final long HEARTBEAT_SECONDS = 30L;

    private final ContestFinalRankService service;
    private final ContestRankProperties properties;
    private final Clock clock;
    private final ThreadPoolExecutor buildExecutor;
    private final ScheduledThreadPoolExecutor heartbeatExecutor;
    private final Set<Long> queuedOrRunning = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);

    public ContestFinalRankJob(ContestFinalRankService service,
                               ContestRankProperties properties,
                               @Qualifier("contestSubmissionClock") Clock clock) {
        this.service = service;
        this.properties = properties;
        this.clock = clock;
        AtomicInteger buildThreadId = new AtomicInteger();
        this.buildExecutor = new ThreadPoolExecutor(BUILD_THREADS, BUILD_THREADS, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(BUILD_QUEUE_CAPACITY), namedFactory("contest-rank-build-", buildThreadId),
                new ThreadPoolExecutor.AbortPolicy());
        this.heartbeatExecutor = new ScheduledThreadPoolExecutor(1,
                namedFactory("contest-rank-heartbeat-", new AtomicInteger()));
        this.heartbeatExecutor.setRemoveOnCancelPolicy(true);
    }

    @Scheduled(fixedDelayString = "${oj.contest-rank.interval-ms:15000}")
    public void pollDueContests() {
        if (!scanInProgress.compareAndSet(false, true)) {
            return;
        }
        try {
            List<Long> due = service.findDueContests(properties.getBatchSize());
            if (due == null || due.isEmpty()) {
                return;
            }
            for (Long contestId : due) {
                if (contestId == null || queuedOrRunning.size() >= BUILD_THREADS + BUILD_QUEUE_CAPACITY) {
                    break;
                }
                if (!queuedOrRunning.add(contestId)) {
                    continue;
                }
                try {
                    // Claim happens inside the runnable, not here: queued work never consumes a DB lease.
                    buildExecutor.execute(() -> process(contestId));
                } catch (RejectedExecutionException rejected) {
                    queuedOrRunning.remove(contestId);
                    log.warn("Final-rank worker queue is full contestId={}", contestId);
                }
            }
        } catch (RuntimeException failure) {
            log.error("Final-rank due scan failed errorType={}", failure.getClass().getSimpleName());
        } finally {
            scanInProgress.set(false);
        }
    }

    private void process(Long contestId) {
        ScheduledFuture<?> heartbeat = null;
        ContestRankBuildLease lease = null;
        AtomicBoolean leaseLost = new AtomicBoolean(false);
        try {
            lease = service.claim(contestId, UUID.randomUUID().toString());
            if (lease == null) {
                return;
            }
            ContestRankBuildLease activeLease = lease;
            heartbeat = heartbeatExecutor.scheduleAtFixedRate(() -> {
                try {
                    if (!service.renewLease(activeLease)) {
                        leaseLost.set(true);
                        log.warn("Final-rank lease was fenced contestId={} candidateVersion={}",
                                activeLease.getContestId(), activeLease.getCandidateVersion());
                    }
                } catch (RuntimeException failure) {
                    // The database lease remains authoritative. Candidate/publish fencing will decide later.
                    log.warn("Final-rank heartbeat failed contestId={} errorType={}",
                            activeLease.getContestId(), failure.getClass().getSimpleName());
                }
            }, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);

            long totalUsers = service.buildCandidate(lease);
            if (!leaseLost.get() && service.publishCandidate(lease, totalUsers)) {
                cleanupPublishedVersions(contestId);
                log.info("Final-rank version published contestId={} version={} rows={} at={}",
                        contestId, lease.getCandidateVersion(), totalUsers, clock.instant());
            }
        } catch (RuntimeException failure) {
            if (lease != null) {
                try {
                    service.markBuildFailed(lease, safeDiagnostic(failure));
                } catch (RuntimeException stateFailure) {
                    log.error("Could not persist final-rank failure contestId={} candidateVersion={} errorType={}",
                            lease.getContestId(), lease.getCandidateVersion(), stateFailure.getClass().getSimpleName());
                }
            }
            log.error("Final-rank build failed contestId={} candidateVersion={} errorType={}",
                    contestId, lease == null ? null : lease.getCandidateVersion(),
                    failure.getClass().getSimpleName());
        } finally {
            if (heartbeat != null) {
                heartbeat.cancel(false);
            }
            queuedOrRunning.remove(contestId);
        }
    }

    private void cleanupPublishedVersions(Long contestId) {
        int removed;
        do {
            removed = service.cleanupVersionBatch(contestId, properties.getCleanupBatchSize());
        } while (removed >= properties.getCleanupBatchSize());
    }

    private String safeDiagnostic(RuntimeException failure) {
        String message = failure.getMessage();
        String diagnostic = failure.getClass().getSimpleName()
                + (message == null || message.trim().isEmpty() ? "" : ": " + message);
        diagnostic = diagnostic.replaceAll("[\\r\\n\\t]", " ").trim();
        return diagnostic.length() <= 1000 ? diagnostic : diagnostic.substring(0, 1000);
    }

    private ThreadFactory namedFactory(String prefix, AtomicInteger threadId) {
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + threadId.incrementAndGet());
            thread.setDaemon(false);
            return thread;
        };
    }

    @PreDestroy
    public void shutdown() {
        heartbeatExecutor.shutdownNow();
        buildExecutor.shutdown();
        try {
            if (!buildExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                buildExecutor.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            buildExecutor.shutdownNow();
        }
    }
}
