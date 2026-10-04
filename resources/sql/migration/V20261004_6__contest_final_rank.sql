-- Forward-only schema for durable contest attempts, frozen roster metadata and rank versions.
-- Do not mark historical submit_log rows as applied here: that requires the maintenance
-- preflight documented at the end of this file and an explicit operator decision.
USE db_problem;

DELIMITER $$
DROP PROCEDURE IF EXISTS prepare_contest_final_rank_t17$$
CREATE PROCEDURE prepare_contest_final_rank_t17()
BEGIN
    DECLARE v_has_unique INT DEFAULT 0;
    DECLARE v_duplicate_count BIGINT DEFAULT 0;

    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = DATABASE() AND table_name = 'contest')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'submit_log')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'contest_records')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'records')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'judge_case_log')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'user_submit')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'problem_list')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'problem_problem_list')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'problem') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'T17 requires contest, submit_log, contest_records and judge_case_log tables';
    END IF;

    SELECT COUNT(*) INTO v_duplicate_count
    FROM contest
    WHERE type = 0 AND (start_time IS NULL OR end_time IS NULL OR start_time >= end_time);
    IF v_duplicate_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'T17 preflight: CONTEST rows have missing/invalid schedule; inspect and resolve before migration';
    END IF;

    SELECT COUNT(*) INTO v_duplicate_count
    FROM (
        SELECT contest_id, user_id, problem_id
        FROM contest_records
        GROUP BY contest_id, user_id, problem_id
        HAVING COUNT(*) > 1
    ) duplicates_found;
    IF v_duplicate_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'T17 preflight: duplicate contest_records (contest_id,user_id,problem_id); preserve and reconcile before migration';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'contest'
                     AND column_name = 'scoring_version') THEN
        ALTER TABLE contest ADD COLUMN scoring_version INT NOT NULL DEFAULT 2
            COMMENT '1=legacy score projection, 2=versioned current scoring';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'contest'
                     AND column_name = 'next_attempt_seq') THEN
        ALTER TABLE contest ADD COLUMN next_attempt_seq BIGINT NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'contest'
                     AND column_name = 'problem_snapshot_at') THEN
        ALTER TABLE contest ADD COLUMN problem_snapshot_at DATETIME(3) NULL;
    END IF;

    -- Re-runnable classification: only version-2 contests that have never accepted
    -- a new formal attempt can be recognized as legacy from old score/submission rows.
    -- Once the new pipeline advances next_attempt_seq (or freezes the roster), a later
    -- manifest rerun must not relabel that contest.
    UPDATE contest c
    SET c.scoring_version = 1
    WHERE c.type = 0 AND c.scoring_version = 2 AND c.next_attempt_seq = 0
      AND c.problem_snapshot_at IS NULL
      AND (
          EXISTS (SELECT 1 FROM submit_log sl WHERE sl.contest_id = c.contest_id)
          OR EXISTS (SELECT 1 FROM contest_records cr WHERE cr.contest_id = c.contest_id)
          OR EXISTS (SELECT 1 FROM records r WHERE r.contest_id = c.contest_id)
          OR EXISTS (SELECT 1 FROM user_submit us WHERE us.contest_id = c.contest_id)
      );

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'submit_log'
                     AND column_name = 'score') THEN
        ALTER TABLE submit_log ADD COLUMN score DECIMAL(10,2) NULL
            COMMENT 'Final score applied to contest projection; legacy value unknown';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'submit_log'
                     AND column_name = 'result_applied') THEN
        ALTER TABLE submit_log ADD COLUMN result_applied BOOLEAN NOT NULL DEFAULT FALSE
            COMMENT 'Whether this terminal result was applied to contest_records';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'submit_log'
                     AND column_name = 'completed_at') THEN
        ALTER TABLE submit_log ADD COLUMN completed_at DATETIME(3) NULL
            COMMENT 'Actual result completion time; not synthesized for legacy rows';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'contest_records'
                     AND column_name = 'applied_attempt_seq') THEN
        ALTER TABLE contest_records ADD COLUMN applied_attempt_seq BIGINT NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'contest_records'
                     AND column_name = 'applied_attempt_id') THEN
        ALTER TABLE contest_records ADD COLUMN applied_attempt_id BIGINT NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'submit_log'
                     AND index_name = 'idx_submit_log_contest_status_time_id') THEN
        ALTER TABLE submit_log ADD INDEX idx_submit_log_contest_status_time_id
            (contest_id, status, submit_time, submit_id);
    END IF;

    -- The existing contest_records unique key is part of the data contract.
    SELECT COUNT(*) INTO v_has_unique
    FROM (
        SELECT index_name, non_unique,
               GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') AS indexed_columns
        FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'contest_records'
        GROUP BY index_name, non_unique
        HAVING non_unique = 0 AND indexed_columns = 'contest_id,user_id,problem_id'
    ) existing_unique;
    IF v_has_unique = 0 THEN
        ALTER TABLE contest_records ADD UNIQUE KEY uk_contest_records_contest_user_problem
            (contest_id, user_id, problem_id);
    END IF;
END$$
DELIMITER ;

CALL prepare_contest_final_rank_t17();
DROP PROCEDURE prepare_contest_final_rank_t17;

-- A durable copy of rows that must be removed to satisfy the new case uniqueness rule.
-- The backup is retained; the forward migration never discards duplicate evidence.
CREATE TABLE IF NOT EXISTS judge_case_log_duplicate_backup_t17 LIKE judge_case_log;
DELIMITER $$
DROP PROCEDURE IF EXISTS add_judge_case_backup_timestamp_t17$$
CREATE PROCEDURE add_judge_case_backup_timestamp_t17()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'judge_case_log_duplicate_backup_t17'
                     AND column_name = 'migration_backed_up_at') THEN
        ALTER TABLE judge_case_log_duplicate_backup_t17
            ADD COLUMN migration_backed_up_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3);
    END IF;
END$$
DELIMITER ;
CALL add_judge_case_backup_timestamp_t17();
DROP PROCEDURE add_judge_case_backup_timestamp_t17;

DELIMITER $$
DROP PROCEDURE IF EXISTS deduplicate_judge_case_logs_t17$$
CREATE PROCEDURE deduplicate_judge_case_logs_t17()
BEGIN
    DECLARE v_duplicate_count BIGINT DEFAULT 0;
    DECLARE v_unique_count INT DEFAULT 0;

    SELECT COUNT(*) INTO v_duplicate_count
    FROM (
        SELECT submit_id, case_index
        FROM judge_case_log
        GROUP BY submit_id, case_index
        HAVING COUNT(*) > 1
    ) duplicates_found;

    IF v_duplicate_count > 0 THEN
        INSERT INTO judge_case_log_duplicate_backup_t17
            (id, submit_id, problem_id, case_id, case_index, status, score, time, memory,
             internal_error, create_time, update_time)
        SELECT ranked.id, ranked.submit_id, ranked.problem_id, ranked.case_id,
               ranked.case_index, ranked.status, ranked.score, ranked.time, ranked.memory,
               ranked.internal_error, ranked.create_time, ranked.update_time
        FROM (
            SELECT j.*, ROW_NUMBER() OVER (
                PARTITION BY j.submit_id, j.case_index
                ORDER BY j.update_time DESC, j.id DESC
            ) AS duplicate_position
            FROM judge_case_log j
        ) ranked
        WHERE ranked.duplicate_position > 1
          AND NOT EXISTS (
              SELECT 1 FROM judge_case_log_duplicate_backup_t17 b WHERE b.id = ranked.id
          );

        DELETE target
        FROM judge_case_log target
        JOIN (
            SELECT id
            FROM (
                SELECT j.id, ROW_NUMBER() OVER (
                    PARTITION BY j.submit_id, j.case_index
                    ORDER BY j.update_time DESC, j.id DESC
                ) AS duplicate_position
                FROM judge_case_log j
            ) ranked
            WHERE ranked.duplicate_position > 1
        ) duplicates_to_remove ON duplicates_to_remove.id = target.id;
    END IF;

    SELECT COUNT(*) INTO v_unique_count
    FROM (
        SELECT index_name, non_unique,
               GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') AS indexed_columns
        FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'judge_case_log'
        GROUP BY index_name, non_unique
        HAVING non_unique = 0 AND indexed_columns = 'submit_id,case_index'
    ) existing_unique;
    IF v_unique_count = 0 THEN
        ALTER TABLE judge_case_log ADD UNIQUE KEY uk_judge_case_log_submit_case
            (submit_id, case_index);
    END IF;
END$$
DELIMITER ;
CALL deduplicate_judge_case_logs_t17();
DROP PROCEDURE deduplicate_judge_case_logs_t17;

CREATE TABLE IF NOT EXISTS contest_problem_snapshot (
    contest_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    problem_order INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    problem_type INT NOT NULL,
    max_score DECIMAL(10,3) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (contest_id, problem_id),
    UNIQUE KEY uk_contest_problem_snapshot_order (contest_id, problem_order),
    CONSTRAINT fk_contest_problem_snapshot_contest FOREIGN KEY (contest_id)
        REFERENCES contest (contest_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_contest_problem_snapshot_values CHECK (problem_order > 0 AND max_score >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Frozen contest roster metadata; not the source of problem body or judge cases';

CREATE TABLE IF NOT EXISTS contest_attempt (
    attempt_id BIGINT NOT NULL COMMENT 'ASSIGN_ID attempt identifier',
    contest_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    attempt_seq BIGINT NOT NULL,
    submit_id BIGINT NULL,
    kind VARCHAR(16) NOT NULL,
    accepted_at DATETIME(3) NOT NULL,
    state VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    judge_status VARCHAR(32) NULL,
    score DECIMAL(10,2) NULL,
    correct BOOLEAN NULL,
    completed_at DATETIME(3) NULL,
    PRIMARY KEY (attempt_id),
    UNIQUE KEY uk_contest_attempt_sequence (contest_id, attempt_seq),
    UNIQUE KEY uk_contest_attempt_submit (submit_id),
    KEY idx_contest_attempt_state_seq (contest_id, state, attempt_seq),
    KEY idx_contest_attempt_user_problem_seq (contest_id, user_id, problem_id, attempt_seq),
    CONSTRAINT chk_contest_attempt_state CHECK (state IN ('PENDING', 'COMPLETED', 'INFRA_ERROR')),
    CONSTRAINT chk_contest_attempt_score CHECK (score IS NULL OR score >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Durable formal contest attempts; source content remains in submit/answer tables';

CREATE TABLE IF NOT EXISTS contest_rank_snapshot (
    contest_id BIGINT NOT NULL,
    state VARCHAR(16) NOT NULL DEFAULT 'WAITING',
    version BIGINT NOT NULL DEFAULT 0,
    next_version BIGINT NOT NULL DEFAULT 0,
    build_attempts INT NOT NULL DEFAULT 0,
    next_build_at DATETIME(3) NOT NULL,
    source_seq BIGINT NOT NULL DEFAULT 0,
    source_mode VARCHAR(24) NOT NULL,
    rule_version VARCHAR(24) NOT NULL,
    total_users BIGINT NOT NULL DEFAULT 0,
    pending_count BIGINT NOT NULL DEFAULT 0,
    lease_owner VARCHAR(64) NULL,
    lease_until DATETIME(3) NULL,
    generated_at DATETIME(3) NULL,
    last_error VARCHAR(1000) NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (contest_id),
    KEY idx_contest_rank_snapshot_due (state, next_build_at, lease_until),
    CONSTRAINT fk_contest_rank_snapshot_contest FOREIGN KEY (contest_id)
        REFERENCES contest (contest_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_contest_rank_snapshot_state CHECK (state IN ('WAITING', 'BUILDING', 'READY', 'ERROR')),
    CONSTRAINT chk_contest_rank_snapshot_counts CHECK (
        version >= 0 AND next_version >= 0 AND build_attempts >= 0
        AND source_seq >= 0 AND total_users >= 0 AND pending_count >= 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Version and lease header for immutable final-rank generations';

CREATE TABLE IF NOT EXISTS contest_rank_entry (
    contest_id BIGINT NOT NULL,
    version BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rank_no BIGINT NOT NULL,
    row_position BIGINT NOT NULL,
    score DECIMAL(18,2) NOT NULL,
    correct_count INT NOT NULL DEFAULT 0,
    answered_count INT NOT NULL DEFAULT 0,
    handed_in BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (contest_id, version, user_id),
    UNIQUE KEY uk_contest_rank_entry_position (contest_id, version, row_position),
    KEY idx_contest_rank_entry_page (contest_id, version, rank_no, row_position),
    CONSTRAINT chk_contest_rank_entry_values CHECK (
        version > 0 AND rank_no > 0 AND row_position > 0 AND score >= 0
        AND correct_count >= 0 AND answered_count >= 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Immutable aggregate entries for one published or candidate rank version';

DELIMITER $$
DROP PROCEDURE IF EXISTS seed_contest_history_t17$$
CREATE PROCEDURE seed_contest_history_t17()
BEGIN
    DECLARE v_invalid_schedule BIGINT DEFAULT 0;
    DECLARE v_invalid_snapshot_source BIGINT DEFAULT 0;
    DECLARE v_seed_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3);
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SELECT COUNT(*) INTO v_invalid_schedule
    FROM contest
    WHERE type = 0 AND (start_time IS NULL OR end_time IS NULL OR start_time >= end_time);
    IF v_invalid_schedule > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'T17 preflight: CONTEST rows have missing/invalid schedule; inspect and resolve before migration';
    END IF;

    SELECT COUNT(*) INTO v_invalid_snapshot_source
    FROM contest c
    LEFT JOIN problem_list pl ON pl.list_id = c.list_id
    LEFT JOIN problem_problem_list ppl ON ppl.list_id = c.list_id
    LEFT JOIN problem p ON p.problem_id = ppl.problem_id
    WHERE c.scoring_version = 1 AND c.problem_snapshot_at IS NULL
      AND (pl.list_id IS NULL OR pl.del_flag <> 0
           OR (ppl.problem_id IS NOT NULL AND (
               p.problem_id IS NULL OR p.del_flag <> 0 OR p.title IS NULL OR TRIM(p.title) = ''
               OR p.type NOT IN (1, 2, 3, 4) OR ppl.score IS NULL OR ppl.score < 0
           )));
    IF v_invalid_snapshot_source > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'T17 preflight: invalid/deleted list problem or score; inspect current roster before migration';
    END IF;

    START TRANSACTION;

    -- This is a deployment-time display snapshot only; it is not represented as the
    -- original roster used to calculate an old result. Reruns do not append rows to
    -- contests whose problem_snapshot_at already records the migration snapshot.
    INSERT IGNORE INTO contest_problem_snapshot
        (contest_id, problem_id, problem_order, title, problem_type, max_score, created_at)
    SELECT roster.contest_id, roster.problem_id,
           ROW_NUMBER() OVER (PARTITION BY roster.contest_id
                              ORDER BY roster.problem_order, roster.problem_id),
           roster.title, roster.problem_type, roster.max_score, v_seed_time
    FROM (
        SELECT c.contest_id, ppl.problem_id, ppl.problem_order, p.title,
               p.type AS problem_type, ppl.score AS max_score
        FROM contest c
        JOIN problem_problem_list ppl ON ppl.list_id = c.list_id
        JOIN problem p ON p.problem_id = ppl.problem_id
        WHERE c.scoring_version = 1 AND c.problem_snapshot_at IS NULL
    ) roster;

    UPDATE contest
    SET problem_snapshot_at = v_seed_time
    WHERE scoring_version = 1 AND problem_snapshot_at IS NULL;

    -- Seed source/mode metadata but never overwrite an existing state or version.
    -- No worker is started by this schema migration.
    INSERT IGNORE INTO contest_rank_snapshot
        (contest_id, state, version, next_version, build_attempts, next_build_at,
         source_seq, source_mode, rule_version, total_users, pending_count,
         lease_owner, lease_until, generated_at, last_error, updated_at)
    SELECT c.contest_id, 'WAITING', 0, 0, 0, v_seed_time, 0,
           IF(c.scoring_version = 1, 'LEGACY', 'CURRENT'),
           IF(c.scoring_version = 1, 'LEGACY_RECORD_V1', 'WEIGHTED_LATEST_V1'),
           0, 0, NULL, NULL, NULL, NULL, v_seed_time
    FROM contest c
    WHERE c.type = 0;
    COMMIT;
END$$
DELIMITER ;
CALL seed_contest_history_t17();
DROP PROCEDURE seed_contest_history_t17;

-- Operator-only preflight before the later result-application cutover. Do not turn
-- these old terminal rows into applied rows until admissions and judge callbacks are
-- stopped and an operator has verified that no old submission can still complete.
-- Leave score/completed_at NULL for history; do not manufacture an old score/time.
SELECT COUNT(*) AS terminal_contest_submissions_requiring_maintenance_review
FROM submit_log
WHERE contest_id IS NOT NULL
  AND status IS NOT NULL
  AND LOWER(status) NOT IN ('queue', 'compiling', 'running')
  AND result_applied = FALSE;

-- Diagnostic to run before migration if it stops for missing schedules:
-- SELECT contest_id, title, start_time, end_time FROM contest
-- WHERE type = 0 AND (start_time IS NULL OR end_time IS NULL OR start_time >= end_time);
