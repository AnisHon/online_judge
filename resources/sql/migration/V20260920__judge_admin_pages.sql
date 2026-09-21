-- 判题管理页菜单与权限迁移。
-- 判题记录是一次完整用户提交；判题日志是逐测试用例的内部诊断信息。
use db_user;

insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component)
values (44, '判题记录', 4, 4, 'judge-submit-log', 'I', 'problem:judge:submit:read', 'DocumentChecked', 'backend/system/judge-submit-log/JudgeSubmitLog')
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

insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component)
values (45, '判题日志', 5, 4, 'judge-case-log', 'I', 'problem:judge:case:read', 'DataAnalysis', 'backend/system/judge-case-log/JudgeCaseLog')
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

insert ignore into sys_role_menu(role_id, menu_id)
values (3, 44), (4, 44), (3, 45), (4, 45);
