package com.anishan.content.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/** Opt-in settings for content community outbox workers. */
@Validated
@ConfigurationProperties(prefix = "oj.content-community-outbox")
public class CommunityWorkerProperties {

    private boolean enabled = false;

    @Min(1)
    @Max(20)
    private int batchSize = 20;

    @Min(1000)
    private long pollIntervalMs = 1000L;

    @Min(60)
    private int leaseSeconds = 60;

    @Min(5)
    @Max(5)
    private int confirmTimeoutSeconds = 5;

    @Min(500)
    @Max(500)
    private int cleanupBatchSize = 500;

    @Min(3600000)
    private long cleanupIntervalMs = 3600000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getLeaseSeconds() {
        return leaseSeconds;
    }

    public void setLeaseSeconds(int leaseSeconds) {
        this.leaseSeconds = leaseSeconds;
    }

    public int getConfirmTimeoutSeconds() {
        return confirmTimeoutSeconds;
    }

    public void setConfirmTimeoutSeconds(int confirmTimeoutSeconds) {
        this.confirmTimeoutSeconds = confirmTimeoutSeconds;
    }

    public int getCleanupBatchSize() {
        return cleanupBatchSize;
    }

    public void setCleanupBatchSize(int cleanupBatchSize) {
        this.cleanupBatchSize = cleanupBatchSize;
    }

    public long getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }

    public void setCleanupIntervalMs(long cleanupIntervalMs) {
        this.cleanupIntervalMs = cleanupIntervalMs;
    }
}
