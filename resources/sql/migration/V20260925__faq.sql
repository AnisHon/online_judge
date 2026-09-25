-- Public FAQ content table and permission-only backend management capabilities.
use db_content;

create table if not exists faq (
    faq_id      bigint(20)      not null primary key comment '主键',
    question    varchar(500)    not null            comment '问题',
    answer      mediumtext      not null            comment '回答',
    del_flag    boolean         not null default 0  comment '逻辑删除',
    create_time datetime        not null default current_timestamp,
    update_time datetime        not null default current_timestamp on update current_timestamp,
    key faq_visible_order_idx (del_flag, faq_id)
) engine=InnoDB default charset=utf8mb4 comment='常见问题表';

use db_user;

-- These are button/resource permissions under system management, not navigable menu entries.
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component)
values
    (440, '列出FAQ', 6, 4, '#', 'B', 'content:faq:list', '#', null),
    (441, '新增FAQ', 7, 4, '#', 'B', 'content:faq:add', '#', null),
    (442, '编辑FAQ', 8, 4, '#', 'B', 'content:faq:edit', '#', null),
    (443, '删除FAQ', 9, 4, '#', 'B', 'content:faq:remove', '#', null)
on duplicate key update
    menu_name = values(menu_name), order_num = values(order_num), parent_id = values(parent_id),
    router = values(router), menu_type = values(menu_type), perms = values(perms), icon = values(icon),
    component = values(component), del_flag = 0;

insert ignore into sys_role_menu(role_id, menu_id)
values (3, 440), (3, 441), (3, 442), (3, 443),
       (4, 440), (4, 441), (4, 442), (4, 443);
