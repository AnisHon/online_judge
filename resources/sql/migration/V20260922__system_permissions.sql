-- 系统管理页的读取边界与文件下载权限迁移。
use db_user;

insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component)
values (411, '读取缓存值', 2, 41, '#', 'B', 'content:cache:read', '#', null)
on duplicate key update
    menu_name = values(menu_name), order_num = values(order_num), parent_id = values(parent_id),
    menu_type = values(menu_type), perms = values(perms), icon = values(icon), del_flag = 0;

insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component)
values (422, '下载文件', 3, 42, '#', 'B', 'content:file:download', '#', null)
on duplicate key update
    menu_name = values(menu_name), order_num = values(order_num), parent_id = values(parent_id),
    menu_type = values(menu_type), perms = values(perms), icon = values(icon), del_flag = 0;

insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component)
values
    (430, '列出公告', 0, 43, '#', 'B', 'content:notice:list', '#', null),
    (431, '添加公告', 1, 43, '#', 'B', 'content:notice:add', '#', null),
    (432, '编辑公告', 2, 43, '#', 'B', 'content:notice:edit', '#', null),
    (433, '删除公告', 3, 43, '#', 'B', 'content:notice:remove', '#', null)
on duplicate key update
    menu_name = values(menu_name), order_num = values(order_num), parent_id = values(parent_id),
    router = values(router), menu_type = values(menu_type), perms = values(perms), icon = values(icon),
    component = values(component), del_flag = 0;

-- 缓存与公告是管理员诊断/维护能力；文件下载保持普通登录用户访问资料的兼容性。
insert ignore into sys_role_menu(role_id, menu_id)
values (3, 410), (3, 411), (3, 412), (4, 410), (4, 411), (4, 412),
       (3, 430), (3, 431), (3, 432), (3, 433),
       (4, 430), (4, 431), (4, 432), (4, 433),
       (1, 422), (2, 422), (3, 422), (4, 422);
