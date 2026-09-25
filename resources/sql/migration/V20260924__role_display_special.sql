-- 角色的内部标识与网站展示名称分离，并标记需要展示身份徽标的角色。
use db_user;

delimiter $$
drop procedure if exists migrate_role_display_special_columns$$
create procedure migrate_role_display_special_columns()
begin
    if not exists (
        select 1 from information_schema.columns
        where table_schema = database()
          and table_name = 'sys_role'
          and column_name = 'display_name'
    ) then
        alter table sys_role add column display_name varchar(50) null comment '面向用户展示的角色名称' after role_name;
    end if;

    if not exists (
        select 1 from information_schema.columns
        where table_schema = database()
          and table_name = 'sys_role'
          and column_name = 'special_role'
    ) then
        alter table sys_role add column special_role boolean not null default 0 comment '是否公开显示为特殊身份标签' after display_name;
    end if;

    update sys_role
       set display_name = role_name
     where display_name is null or trim(display_name) = '';

    update sys_role
       set display_name = case role_name
               when 'student' then '学生'
               when 'teacher' then '教师'
               when 'admin' then '管理员'
               when 'super_admin' then '超级管理员'
               else display_name
           end,
           special_role = case role_name
               when 'teacher' then 1
               when 'admin' then 1
               when 'super_admin' then 1
               else special_role
           end
     where role_name in ('student', 'teacher', 'admin', 'super_admin');

    alter table sys_role
        modify column display_name varchar(50) not null comment '面向用户展示的角色名称';
end$$
delimiter ;

call migrate_role_display_special_columns();
drop procedure migrate_role_display_special_columns;
