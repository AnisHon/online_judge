-- Run manually against db_user after deploying the matching services. No automatic role creation.
-- This migration is idempotent; it does not touch authentication, step-up or activity data.
use db_user;
start transaction;

-- Retire only confirmed ordinary/front-facing permissions. Management permissions are retained.
delete rm from sys_role_menu rm join sys_menu m on m.menu_id = rm.menu_id
where m.perms in ('problem:submit:read', 'content:file:download', 'content:info');
update sys_menu set del_flag = 1
where perms in ('problem:submit:read', 'content:file:download', 'content:info');

-- Standalone upload/rename permissions must not drag a front-facing capability under a backend route.
-- They still protect the existing managed file workflow; do not retire them or file removal.
update sys_menu set parent_id = null
where menu_type = 'B' and perms in ('content:file:add', 'content:file:edit');

insert into sys_menu(menu_name, order_num, parent_id, router, menu_type, perms, icon, remark)
select p.menu_name, p.order_num, null, '#', 'B', p.perms, '#', '角色承载的禁止策略；优先拒绝写入，不依赖后台路由'
from (
    select '禁止题解写入' as menu_name, 1200 as order_num, 'policy:solution:deny' as perms
    union all select '禁止评论写入（预留）', 1201, 'policy:comment:deny'
    union all select '禁止更换头像', 1202, 'policy:avatar:deny'
) p
where not exists (select 1 from sys_menu m where m.perms = p.perms);

update sys_menu set parent_id = null, router = '#', component = null, menu_type = 'B', del_flag = 0
where perms in ('policy:solution:deny', 'policy:comment:deny', 'policy:avatar:deny');

insert into sys_menu(menu_name, order_num, parent_id, router, menu_type, perms, icon)
select '重置用户头像', 7,
       (select menu_id from sys_menu where router = 'user-manage' and menu_type = 'I' and del_flag = 0 order by menu_id limit 1),
       '#', 'B', 'user:user:reset-avatar', '#'
where not exists (select 1 from sys_menu where perms = 'user:user:reset-avatar');
update sys_menu set del_flag = 0 where perms = 'user:user:reset-avatar';

-- Default administrators receive the positive moderation permission only.
-- No role receives a deny permission here; administrators explicitly configure those assignments.
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id from sys_role r join sys_menu m on m.perms = 'user:user:reset-avatar'
where r.role_name in ('admin', 'super_admin') and r.del_flag = 0 and m.del_flag = 0;
commit;

-- After execution: an authorized administrator calls DELETE /user-api/role/refresh to refresh
-- role/menu caches AND existing LoginUser snapshots in the matching deployment.
-- SQL alone cannot refresh Redis. Existing frontend tabs may need to reload their user snapshot.
