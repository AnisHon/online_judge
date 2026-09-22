-- 为后台入口增加独立权限，避免前端用菜单树是否为空推断访问能力。
use db_user;

insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component)
values (1004, '进入后台', 4, null, '#', 'B', 'system:backend:access', '#', null)
on duplicate key update
    menu_name = values(menu_name),
    order_num = values(order_num),
    parent_id = values(parent_id),
    router = values(router),
    menu_type = values(menu_type),
    perms = values(perms),
    icon = values(icon),
    component = values(component),
    del_flag = 0;

-- 教师、管理员和超级管理员可以进入后台；普通学生不授予该权限。
insert ignore into sys_role_menu(role_id, menu_id)
values (2, 1004), (3, 1004), (4, 1004);
