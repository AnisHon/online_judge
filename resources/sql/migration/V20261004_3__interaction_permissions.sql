-- Add real button permissions beneath existing routes; do not create route-less pages.
-- Role/menu IDs are resolved by role_name, router and perms for this installation.
USE db_user;

DROP PROCEDURE IF EXISTS migrate_interaction_permissions_t02;
DELIMITER $$
CREATE PROCEDURE migrate_interaction_permissions_t02()
BEGIN
    DECLARE solution_route_id BIGINT;
    DECLARE judge_route_id BIGINT;
    DECLARE required_count INT DEFAULT 0;
    DECLARE transaction_started BOOLEAN DEFAULT FALSE;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        IF transaction_started THEN
            ROLLBACK;
        END IF;
        RESIGNAL;
    END;

    SELECT COUNT(*), MIN(menu_id) INTO required_count, solution_route_id
    FROM sys_menu
    WHERE router = 'solution-edit' AND menu_type = 'I' AND del_flag = 0;
    IF required_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'T02 migration requires exactly one active I route with router=solution-edit';
    END IF;

    SELECT COUNT(*), MIN(menu_id) INTO required_count, judge_route_id
    FROM sys_menu
    WHERE router = 'judge-submit-log' AND menu_type = 'I' AND del_flag = 0;
    IF required_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'T02 migration requires exactly one active I route with router=judge-submit-log';
    END IF;

    SELECT COUNT(*) INTO required_count
    FROM sys_menu WHERE perms = 'problem:contest:rank' AND menu_type = 'B' AND del_flag = 0;
    IF required_count < 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'T02 migration requires the existing problem:contest:rank button permission';
    END IF;

    SELECT COUNT(*) INTO required_count
    FROM sys_menu WHERE perms = 'system:backend:access' AND menu_type = 'B' AND del_flag = 0;
    IF required_count < 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'T02 migration requires the existing system:backend:access button permission';
    END IF;

    SELECT COUNT(*) INTO required_count
    FROM sys_role WHERE role_name IN ('admin', 'super_admin') AND del_flag = 0;
    IF required_count <> 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'T02 migration requires active admin and super_admin roles';
    END IF;

    START TRANSACTION;
    SET transaction_started = TRUE;

    INSERT INTO sys_menu(menu_name, order_num, parent_id, router, menu_type, perms, icon, component)
    SELECT '查看评论', 5, solution_route_id, '#', 'B', 'problem:comment:list', '#', NULL
    WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'problem:comment:list');

    INSERT INTO sys_menu(menu_name, order_num, parent_id, router, menu_type, perms, icon, component)
    SELECT '删除评论', 6, solution_route_id, '#', 'B', 'problem:comment:remove', '#', NULL
    WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'problem:comment:remove');

    INSERT INTO sys_menu(menu_name, order_num, parent_id, router, menu_type, perms, icon, component)
    SELECT '重试判题派发', 6, judge_route_id, '#', 'B', 'problem:judge:dispatch:retry', '#', NULL
    WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'problem:judge:dispatch:retry');

    -- Reuse an existing permission row by perms and repair only its expected type/parent metadata.
    UPDATE sys_menu
    SET menu_name = '查看评论', order_num = 5, parent_id = solution_route_id,
        router = '#', menu_type = 'B', icon = '#', component = NULL, del_flag = 0
    WHERE perms = 'problem:comment:list';

    UPDATE sys_menu
    SET menu_name = '删除评论', order_num = 6, parent_id = solution_route_id,
        router = '#', menu_type = 'B', icon = '#', component = NULL, del_flag = 0
    WHERE perms = 'problem:comment:remove';

    UPDATE sys_menu
    SET menu_name = '重试判题派发', order_num = 6, parent_id = judge_route_id,
        router = '#', menu_type = 'B', icon = '#', component = NULL, del_flag = 0
    WHERE perms = 'problem:judge:dispatch:retry';

    -- The denial policy is real, but its label is no longer described as a placeholder.
    UPDATE sys_menu SET menu_name = '禁止评论写入'
    WHERE perms = 'policy:comment:deny';

    -- Grant only the requested positive capabilities plus their actual ancestor chain.
    -- Existing teacher grants (including contest rank) are not deleted or rewritten.
    INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
    WITH RECURSIVE role_menu_path(role_id, menu_id, parent_id) AS (
        SELECT r.role_id, m.menu_id, m.parent_id
        FROM sys_role r
        JOIN sys_menu m ON m.perms IN (
            'problem:comment:list', 'problem:comment:remove',
            'problem:judge:dispatch:retry', 'problem:contest:rank', 'system:backend:access'
        ) AND m.del_flag = 0
        WHERE r.role_name IN ('admin', 'super_admin') AND r.del_flag = 0
        UNION DISTINCT
        SELECT path.role_id, parent.menu_id, parent.parent_id
        FROM role_menu_path path
        JOIN sys_menu parent ON parent.menu_id = path.parent_id AND parent.del_flag = 0
    )
    SELECT role_id, menu_id FROM role_menu_path;

    COMMIT;
    SET transaction_started = FALSE;
END$$
DELIMITER ;

CALL migrate_interaction_permissions_t02();
DROP PROCEDURE migrate_interaction_permissions_t02;
