package com.anishan.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/** Opt-in switches and bounded fanout settings for user-owned MQ consumers. */
@Validated
@ConfigurationProperties(prefix = "oj.notification-worker")
public class NotificationWorkerProperties {

    private boolean communityEnabled = false;
    private boolean pointsEnabled = false;

    @Min(1)
    @Max(200)
    private int fanoutBatchSize = 200;

    @Min(1000)
    private long fanoutPollIntervalMs = 1000L;

    @Min(60)
    @Max(60)
    private int leaseSeconds = 60;

    public boolean isCommunityEnabled() {
        return communityEnabled;
    }

    public void setCommunityEnabled(boolean communityEnabled) {
        this.communityEnabled = communityEnabled;
    }

    public boolean isPointsEnabled() {
        return pointsEnabled;
    }

    public void setPointsEnabled(boolean pointsEnabled) {
        this.pointsEnabled = pointsEnabled;
    }

    public int getFanoutBatchSize() {
        return fanoutBatchSize;
    }

    public void setFanoutBatchSize(int fanoutBatchSize) {
        this.fanoutBatchSize = fanoutBatchSize;
    }

    public long getFanoutPollIntervalMs() {
        return fanoutPollIntervalMs;
    }

    public void setFanoutPollIntervalMs(long fanoutPollIntervalMs) {
        this.fanoutPollIntervalMs = fanoutPollIntervalMs;
    }

    public int getLeaseSeconds() {
        return leaseSeconds;
    }

    public void setLeaseSeconds(int leaseSeconds) {
        this.leaseSeconds = leaseSeconds;
    }
}
