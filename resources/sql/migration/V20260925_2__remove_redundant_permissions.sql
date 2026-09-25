-- Retire redundant menu permissions without changing the underlying access behavior.
use db_user;

-- Personal submission APIs require an authenticated user and verify submit ownership.
-- Materials downloads are shared by all regular roles; require authentication, not an RBAC permission.
delete from sys_role_menu where menu_id in (150, 422);
update sys_menu
set del_flag = 1
where menu_id = 150 and perms = 'problem:submit:read';
update sys_menu
set del_flag = 1
where menu_id = 422 and perms = 'content:file:download';
