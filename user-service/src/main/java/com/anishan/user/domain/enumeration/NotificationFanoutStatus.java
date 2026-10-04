package com.anishan.user.domain.enumeration;

/** Durable state of one follower notification fanout job. */
public enum NotificationFanoutStatus {
    PENDING,
    SENDING,
    DONE,
    FAILED
}
