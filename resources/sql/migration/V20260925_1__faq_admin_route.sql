-- Give FAQ management a real settings page route and nest its operation permissions beneath it.
use db_user;

insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component)
values (46, 'FAQ管理', 6, 4, 'faq-manage', 'I', '#', 'QuestionFilled', 'backend/system/faq-manage/FaqManage')
on duplicate key update
    menu_name = values(menu_name), order_num = values(order_num), parent_id = values(parent_id),
    router = values(router), menu_type = values(menu_type), perms = values(perms),
    icon = values(icon), component = values(component), del_flag = 0;

insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component)
values
    (440, '查看FAQ', 1, 46, '#', 'B', 'content:faq:list', '#', null),
    (441, '新增FAQ', 2, 46, '#', 'B', 'content:faq:add', '#', null),
    (442, '编辑FAQ', 3, 46, '#', 'B', 'content:faq:edit', '#', null),
    (443, '删除FAQ', 4, 46, '#', 'B', 'content:faq:remove', '#', null)
on duplicate key update
    menu_name = values(menu_name), order_num = values(order_num), parent_id = values(parent_id),
    router = values(router), menu_type = values(menu_type), perms = values(perms), icon = values(icon),
    component = values(component), del_flag = 0;

-- FAQ management is a default admin capability, never part of the student/teacher role grants.
delete from sys_role_menu where menu_id in (46, 440, 441, 442, 443) and role_id not in (3, 4);
insert ignore into sys_role_menu(role_id, menu_id)
values (3, 46), (3, 440), (3, 441), (3, 442), (3, 443),
       (4, 46), (4, 440), (4, 441), (4, 442), (4, 443);
