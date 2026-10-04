-- Preserve contests, frozen rosters and historical score records when a problem list
-- is physically deleted. This forward migration is repeatable and does not remove data.
USE db_problem;
DELIMITER $$
DROP PROCEDURE IF EXISTS ensure_t18c_restrict_fk$$
CREATE PROCEDURE ensure_t18c_restrict_fk(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_referenced_table VARCHAR(64),
    IN p_referenced_column VARCHAR(64),
    IN p_expected_constraint VARCHAR(64)
)
BEGIN
    DECLARE v_constraint_name VARCHAR(64) DEFAULT NULL;
    DECLARE v_delete_rule VARCHAR(16) DEFAULT NULL;
    DECLARE v_update_rule VARCHAR(16) DEFAULT NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_constraint_name = NULL;

    SELECT rc.constraint_name, rc.delete_rule, rc.update_rule
      INTO v_constraint_name, v_delete_rule, v_update_rule
    FROM information_schema.referential_constraints rc
    INNER JOIN information_schema.key_column_usage kcu
      ON kcu.constraint_schema = rc.constraint_schema
     AND kcu.table_name = rc.table_name
     AND kcu.constraint_name = rc.constraint_name
    WHERE rc.constraint_schema = DATABASE()
      AND rc.table_name = p_table_name
      AND kcu.column_name = p_column_name
      AND kcu.referenced_table_name = p_referenced_table
      AND kcu.referenced_column_name = p_referenced_column
    LIMIT 1;

    IF v_constraint_name IS NOT NULL
       AND (v_delete_rule <> 'RESTRICT' OR v_update_rule <> 'RESTRICT') THEN
        SET @t18c_drop_fk_sql = CONCAT(
            'ALTER TABLE `', REPLACE(p_table_name, '`', '``'), '` DROP FOREIGN KEY `',
            REPLACE(v_constraint_name, '`', '``'), '`'
        );
        PREPARE t18c_drop_fk_stmt FROM @t18c_drop_fk_sql;
        EXECUTE t18c_drop_fk_stmt;
        DEALLOCATE PREPARE t18c_drop_fk_stmt;
        SET v_constraint_name = NULL;
    END IF;

    IF v_constraint_name IS NULL THEN
        SET @t18c_add_fk_sql = CONCAT(
            'ALTER TABLE `', REPLACE(p_table_name, '`', '``'), '` ADD CONSTRAINT `',
            REPLACE(p_expected_constraint, '`', '``'), '` FOREIGN KEY (`',
            REPLACE(p_column_name, '`', '``'), '`) REFERENCES `',
            REPLACE(p_referenced_table, '`', '``'), '` (`',
            REPLACE(p_referenced_column, '`', '``'), '`) ON DELETE RESTRICT ON UPDATE RESTRICT'
        );
        PREPARE t18c_add_fk_stmt FROM @t18c_add_fk_sql;
        EXECUTE t18c_add_fk_stmt;
        DEALLOCATE PREPARE t18c_add_fk_stmt;
    END IF;
END$$

CALL ensure_t18c_restrict_fk('contest', 'list_id', 'problem_list', 'list_id', 'contest_list_id_fk')$$
CALL ensure_t18c_restrict_fk('contest_records', 'contest_id', 'contest', 'contest_id', 'contest_records_contest_id_fk')$$
CALL ensure_t18c_restrict_fk('contest_records', 'problem_id', 'problem', 'problem_id', 'contest_records_problem_id_fk')$$
DROP PROCEDURE ensure_t18c_restrict_fk$$
DELIMITER ;
