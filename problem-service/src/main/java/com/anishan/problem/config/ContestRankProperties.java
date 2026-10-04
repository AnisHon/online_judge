package com.anishan.problem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Component
@Validated
@ConfigurationProperties(prefix = "oj.contest-rank")
public class ContestRankProperties {

    private boolean enabled;

    @Min(15000)
    @Max(15000)
    private long intervalMs = 15000L;

    @Min(1)
    @Max(20)
    private int batchSize = 20;

    @Min(120)
    @Max(120)
    private int leaseSeconds = 120;

    @Min(500)
    @Max(500)
    private int cleanupBatchSize = 500;

    @Min(60)
    @Max(60)
    private int queryTimeoutSeconds = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getLeaseSeconds() {
        return leaseSeconds;
    }

    public void setLeaseSeconds(int leaseSeconds) {
        this.leaseSeconds = leaseSeconds;
    }

    public int getCleanupBatchSize() {
        return cleanupBatchSize;
    }

    public void setCleanupBatchSize(int cleanupBatchSize) {
        this.cleanupBatchSize = cleanupBatchSize;
    }

    public int getQueryTimeoutSeconds() {
        return queryTimeoutSeconds;
    }

    public void setQueryTimeoutSeconds(int queryTimeoutSeconds) {
        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }
}
