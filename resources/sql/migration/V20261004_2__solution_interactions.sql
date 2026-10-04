-- Add solution interaction/audit/outbox state.
-- DDL is intentionally idempotent but not transactionally reversible on MySQL.
-- Run this before deploying code that reads first_published_at or the new tables.
USE db_problem;

SET @oj_t02_backfill_cutoff = NOW(3);
SET @oj_t02_backfill_first_published = 0;

DROP PROCEDURE IF EXISTS migrate_solution_interactions_t02;
DELIMITER $$
CREATE PROCEDURE migrate_solution_interactions_t02()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'solution_explanation' AND column_name = 'del_flag'
    ) THEN
        ALTER TABLE solution_explanation
            ADD COLUMN del_flag BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Logical deletion flag' AFTER `private`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'solution_explanation' AND column_name = 'moderation_state'
    ) THEN
        ALTER TABLE solution_explanation
            ADD COLUMN moderation_state VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'Administrative visibility state' AFTER del_flag;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'solution_explanation' AND column_name = 'comments_open'
    ) THEN
        ALTER TABLE solution_explanation
            ADD COLUMN comments_open BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether new comments are allowed' AFTER moderation_state;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'solution_explanation' AND column_name = 'like_count'
    ) THEN
        ALTER TABLE solution_explanation
            ADD COLUMN like_count BIGINT NOT NULL DEFAULT 0 COMMENT 'Active like count' AFTER comments_open;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'solution_explanation' AND column_name = 'comment_count'
    ) THEN
        ALTER TABLE solution_explanation
            ADD COLUMN comment_count BIGINT NOT NULL DEFAULT 0 COMMENT 'Active comment count' AFTER like_count;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'solution_explanation' AND column_name = 'first_published_at'
    ) THEN
        ALTER TABLE solution_explanation
            ADD COLUMN first_published_at DATETIME(3) NULL
                COMMENT 'T02_BACKFILL_PENDING' AFTER comment_count;
        SET @oj_t02_backfill_first_published = 1;
    ELSEIF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'solution_explanation'
          AND column_name = 'first_published_at' AND column_comment = 'T02_BACKFILL_PENDING'
    ) THEN
        -- A previous run may have stopped after DDL but before the historical backfill completed.
        SET @oj_t02_backfill_first_published = 1;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'solution_explanation' AND column_name = 'version'
    ) THEN
        ALTER TABLE solution_explanation
            ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic concurrency version' AFTER first_published_at;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'solution_explanation'
          AND index_name = 'solution_explanation_visibility_idx'
    ) THEN
        ALTER TABLE solution_explanation
            ADD INDEX solution_explanation_visibility_idx
                (del_flag, moderation_state, `private`, create_time, solution_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE() AND table_name = 'solution_explanation'
          AND constraint_name = 'chk_solution_explanation_moderation_state'
    ) THEN
        ALTER TABLE solution_explanation ADD CONSTRAINT chk_solution_explanation_moderation_state
            CHECK (moderation_state IN ('NORMAL', 'AUTHOR_ONLY'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE() AND table_name = 'solution_explanation'
          AND constraint_name = 'chk_solution_explanation_counts_nonnegative'
    ) THEN
        ALTER TABLE solution_explanation ADD CONSTRAINT chk_solution_explanation_counts_nonnegative
            CHECK (like_count >= 0 AND comment_count >= 0 AND `version` >= 0);
    END IF;
END$$
DELIMITER ;

CALL migrate_solution_interactions_t02();
DROP PROCEDURE migrate_solution_interactions_t02;

-- This data backfill is deliberately separate from DDL: MySQL DDL implicitly commits.
-- The pending column comment makes an interrupted first run resumable. Once finalized,
-- later executions never mark newly published solutions as historical.
UPDATE solution_explanation s
JOIN problem p ON p.problem_id = s.problem_id
SET s.first_published_at = s.create_time
WHERE @oj_t02_backfill_first_published = 1
  AND s.first_published_at IS NULL
  AND s.create_time <= @oj_t02_backfill_cutoff
  AND s.del_flag = 0
  AND s.private = 0
  AND s.moderation_state = 'NORMAL'
  AND p.del_flag = 0
  AND p.auth = 1;

-- Finalize only when this migration owns the one-time backfill marker.
DELIMITER $$
CREATE PROCEDURE finalize_solution_interactions_t02()
BEGIN
    IF @oj_t02_backfill_first_published = 1 THEN
        ALTER TABLE solution_explanation
            MODIFY COLUMN first_published_at DATETIME(3) NULL COMMENT 'First public publication time';
    END IF;
END$$
DELIMITER ;

CALL finalize_solution_interactions_t02();
DROP PROCEDURE finalize_solution_interactions_t02;

CREATE TABLE IF NOT EXISTS solution_like (
    solution_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    first_notified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (solution_id, user_id),
    KEY idx_solution_like_user (user_id, solution_id),
    CONSTRAINT chk_solution_like_first_notification CHECK (first_notified IN (0, 1)),
    CONSTRAINT fk_solution_like_solution FOREIGN KEY (solution_id)
        REFERENCES solution_explanation (solution_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Solution like state; inactive rows are retained for notification idempotency';

CREATE TABLE IF NOT EXISTS solution_comment (
    comment_id BIGINT NOT NULL COMMENT 'ASSIGN_ID comment identifier',
    solution_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    root_id BIGINT NULL COMMENT 'NULL for a root comment; otherwise the top-level comment ID',
    parent_id BIGINT NULL COMMENT 'The directly replied-to comment ID',
    reply_to_user_id BIGINT NULL,
    content VARCHAR(4000) NOT NULL,
    state VARCHAR(16) NOT NULL DEFAULT 'VISIBLE',
    deleted_by BIGINT NULL,
    deleted_at DATETIME(3) NULL,
    like_count BIGINT NOT NULL DEFAULT 0,
    reply_count BIGINT NOT NULL DEFAULT 0,
    client_request_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (comment_id),
    UNIQUE KEY uk_solution_comment_request (user_id, client_request_id),
    KEY idx_solution_comment_solution_root_created (solution_id, root_id, created_at, comment_id),
    KEY idx_solution_comment_root_created (root_id, created_at, comment_id),
    KEY idx_solution_comment_user_created (user_id, created_at),
    CONSTRAINT chk_solution_comment_state CHECK (state IN ('VISIBLE', 'AUTHOR_DELETED', 'ADMIN_DELETED')),
    CONSTRAINT chk_solution_comment_counts_nonnegative CHECK (like_count >= 0 AND reply_count >= 0),
    CONSTRAINT fk_solution_comment_solution FOREIGN KEY (solution_id)
        REFERENCES solution_explanation (solution_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Plain-text solution comments with fixed two-level display semantics';

CREATE TABLE IF NOT EXISTS comment_like (
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    first_notified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (comment_id, user_id),
    KEY idx_comment_like_user (user_id, comment_id),
    CONSTRAINT chk_comment_like_first_notification CHECK (first_notified IN (0, 1)),
    CONSTRAINT fk_comment_like_comment FOREIGN KEY (comment_id)
        REFERENCES solution_comment (comment_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Comment like state; inactive rows are retained for notification idempotency';

CREATE TABLE IF NOT EXISTS solution_moderation_action (
    action_id BIGINT NOT NULL COMMENT 'ASSIGN_ID moderation action identifier',
    solution_id BIGINT NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (action_id),
    KEY idx_solution_moderation_target (target_type, target_id, action_id),
    KEY idx_solution_moderation_author (author_id, action_id),
    CONSTRAINT chk_solution_moderation_target_type CHECK (target_type IN ('SOLUTION', 'COMMENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Append-only solution and comment moderation audit';

CREATE TABLE IF NOT EXISTS problem_event_outbox (
    event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    dedupe_key VARCHAR(160) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    event_type VARCHAR(48) NOT NULL,
    exchange_name VARCHAR(100) NOT NULL,
    routing_key VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(3) NOT NULL,
    lease_owner VARCHAR(64) NULL,
    lease_until DATETIME(3) NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL,
    sent_at DATETIME(3) NULL,
    PRIMARY KEY (event_id),
    UNIQUE KEY uk_problem_event_outbox_dedupe (dedupe_key),
    KEY idx_problem_event_outbox_due (status, next_attempt_at, lease_until),
    CONSTRAINT chk_problem_event_outbox_status CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED')),
    CONSTRAINT chk_problem_event_outbox_attempts CHECK (attempts >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Transactional problem-service message outbox';
