-- Add the user-side social, notification, fanout, and point receipt tables.
-- Safe to rerun: every object is a new table and no existing table or row is altered.
USE db_user;

CREATE TABLE IF NOT EXISTS user_follow (
    follower_id BIGINT NOT NULL,
    followee_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (follower_id, followee_id),
    KEY idx_user_follow_followee (followee_id, follower_id, created_at),
    KEY idx_user_follow_follower_created (follower_id, created_at, followee_id),
    CONSTRAINT chk_user_follow_not_self CHECK (follower_id <> followee_id),
    CONSTRAINT fk_user_follow_follower FOREIGN KEY (follower_id)
        REFERENCES sys_user (user_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_user_follow_followee FOREIGN KEY (followee_id)
        REFERENCES sys_user (user_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Directed user follow relationships';

CREATE TABLE IF NOT EXISTS user_notification (
    notification_id BIGINT NOT NULL COMMENT 'ASSIGN_ID notification identifier',
    recipient_id BIGINT NOT NULL,
    actor_id BIGINT NULL,
    event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    dedupe_key VARCHAR(160) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    type VARCHAR(32) NOT NULL,
    solution_id BIGINT NULL,
    comment_id BIGINT NULL,
    action VARCHAR(32) NULL,
    reason VARCHAR(500) NULL,
    occurred_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    read_at DATETIME(3) NULL,
    PRIMARY KEY (notification_id),
    UNIQUE KEY uk_user_notification_recipient_dedupe (recipient_id, dedupe_key),
    KEY idx_user_notification_recipient_unread (recipient_id, read_at, notification_id),
    KEY idx_user_notification_recipient_page (recipient_id, notification_id),
    KEY idx_user_notification_actor (actor_id),
    CONSTRAINT fk_user_notification_recipient FOREIGN KEY (recipient_id)
        REFERENCES sys_user (user_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_user_notification_actor FOREIGN KEY (actor_id)
        REFERENCES sys_user (user_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Private per-user notifications';

CREATE TABLE IF NOT EXISTS notification_fanout_job (
    event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    actor_id BIGINT NOT NULL,
    solution_id BIGINT NOT NULL,
    occurred_at DATETIME(3) NOT NULL,
    cursor_user_id BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    lease_owner VARCHAR(64) NULL,
    lease_until DATETIME(3) NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(3) NOT NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (event_id),
    KEY idx_notification_fanout_due (status, next_attempt_at, lease_until),
    CONSTRAINT fk_notification_fanout_actor FOREIGN KEY (actor_id)
        REFERENCES sys_user (user_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Resumable follower notification fanout';

CREATE TABLE IF NOT EXISTS user_point_award_receipt (
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (user_id, problem_id),
    UNIQUE KEY uk_user_point_award_event (event_id),
    CONSTRAINT fk_user_point_award_user FOREIGN KEY (user_id)
        REFERENCES sys_user (user_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Idempotent first-AC point award receipts';
