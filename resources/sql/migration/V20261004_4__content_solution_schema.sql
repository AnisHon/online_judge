-- Prepare the content-owned solution schema. This is schema-only; it never reads
-- or writes db_problem and must not be run by the ordinary deployment phase.
USE db_content;

CREATE TABLE IF NOT EXISTS solution_explanation (
    solution_id BIGINT NOT NULL COMMENT '主键',
    title VARCHAR(255) NOT NULL COMMENT '题解标题',
    problem_id BIGINT NOT NULL COMMENT '对应题目（db_problem 外部 ID）',
    user_id BIGINT NOT NULL COMMENT '作者ID',
    top_up BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否置顶',
    `private` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否作者私有',
    del_flag BOOLEAN NOT NULL DEFAULT FALSE COMMENT '逻辑删除标记',
    moderation_state VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT '管理可见性状态',
    comments_open BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否允许新增评论',
    like_count BIGINT NOT NULL DEFAULT 0 COMMENT '有效点赞数',
    comment_count BIGINT NOT NULL DEFAULT 0 COMMENT '有效评论数',
    first_published_at DATETIME(3) NULL COMMENT '首次公开时间',
    `version` BIGINT NOT NULL DEFAULT 0 COMMENT '并发版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (solution_id),
    KEY solution_explanation_user_create_idx (user_id, create_time, solution_id),
    KEY solution_explanation_visibility_idx (del_flag, moderation_state, `private`, create_time, solution_id),
    CONSTRAINT chk_solution_explanation_moderation_state
        CHECK (moderation_state IN ('NORMAL', 'AUTHOR_ONLY')),
    CONSTRAINT chk_solution_explanation_counts_nonnegative
        CHECK (like_count >= 0 AND comment_count >= 0 AND `version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题解主表';

CREATE TABLE IF NOT EXISTS solution_explanation_content (
    solution_id BIGINT NOT NULL COMMENT '题解ID',
    content TEXT NOT NULL COMMENT '题解正文',
    PRIMARY KEY (solution_id),
    CONSTRAINT solution_explanation_content_id_fk FOREIGN KEY (solution_id)
        REFERENCES solution_explanation (solution_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题解正文';

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
  COMMENT='题解点赞关系；取消点赞保留记录以实现通知幂等';

CREATE TABLE IF NOT EXISTS solution_comment (
    comment_id BIGINT NOT NULL COMMENT 'ASSIGN_ID comment identifier',
    solution_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    root_id BIGINT NULL COMMENT 'NULL 为一级评论，否则为根评论ID',
    parent_id BIGINT NULL COMMENT '直接回复的评论ID',
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
  COMMENT='纯文本题解评论；展示固定两层';

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
  COMMENT='评论点赞关系；取消点赞保留记录以实现通知幂等';

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
  COMMENT='题解和评论管理审计，不级联删除';

CREATE TABLE IF NOT EXISTS content_event_outbox (
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
    UNIQUE KEY uk_content_event_outbox_dedupe (dedupe_key),
    KEY idx_content_event_outbox_due (status, next_attempt_at, lease_until),
    CONSTRAINT chk_content_event_outbox_status CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED')),
    CONSTRAINT chk_content_event_outbox_attempts CHECK (attempts >= 0),
    CONSTRAINT chk_content_event_outbox_type CHECK (event_type IN (
        'SOLUTION_PUBLISHED', 'SOLUTION_LIKED', 'SOLUTION_COMMENTED', 'COMMENT_REPLIED',
        'COMMENT_LIKED', 'SOLUTION_MODERATED', 'COMMENT_MODERATED'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='content-service 社区事件事务 outbox';

CREATE TABLE IF NOT EXISTS solution_problem_reference (
    problem_id BIGINT NOT NULL,
    exists_flag BOOLEAN NOT NULL,
    del_flag BOOLEAN NOT NULL,
    auth INT NULL,
    title VARCHAR(255) NULL,
    checked_at DATETIME(3) NOT NULL,
    requested_at DATETIME(3) NOT NULL,
    PRIMARY KEY (problem_id),
    KEY idx_solution_problem_reference_checked (checked_at, problem_id),
    KEY idx_solution_problem_reference_visibility (exists_flag, del_flag, auth, problem_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题解关联题目的只读元数据投影';

CREATE TABLE IF NOT EXISTS content_solution_migration_state (
    migration_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_mode VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    manifest_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    target_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    counts_json JSON NULL,
    started_at DATETIME(3) NOT NULL,
    verified_at DATETIME(3) NULL,
    cutover_at DATETIME(3) NULL,
    PRIMARY KEY (migration_key),
    CONSTRAINT chk_content_solution_migration_source CHECK (source_mode IN ('LEGACY', 'FRESH')),
    CONSTRAINT chk_content_solution_migration_status
        CHECK (status IN ('PREPARED', 'COPIED', 'VERIFIED', 'CUTOVER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题解域维护迁移状态';
