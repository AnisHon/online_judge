-- 用户主页资料迁移：开放个性签名，并记录最近一次成功登录时间。
-- 可重复执行，不删除、不覆盖线上已有用户数据。
use db_user;

delimiter $$
drop procedure if exists migrate_user_profile_columns$$
create procedure migrate_user_profile_columns()
begin
    if not exists (
        select 1 from information_schema.columns
        where table_schema = database()
          and table_name = 'sys_user'
          and column_name = 'signature'
    ) then
        alter table sys_user add column signature varchar(160) null comment '公开个性签名' after nike_name;
    end if;

    if not exists (
        select 1 from information_schema.columns
        where table_schema = database()
          and table_name = 'sys_user'
          and column_name = 'last_login_time'
    ) then
        alter table sys_user add column last_login_time datetime null comment '最近一次成功登录时间' after create_time;
    end if;
end$$
delimiter ;

call migrate_user_profile_columns();
drop procedure migrate_user_profile_columns;

use db_problem;

delimiter $$
drop procedure if exists migrate_submit_log_user_id_type$$
create procedure migrate_submit_log_user_id_type()
begin
    if exists (
        select 1 from submit_log
        where user_id is null
           or trim(user_id) = ''
           or user_id not regexp '^[0-9]+$'
    ) then
        signal sqlstate '45000'
            set message_text = 'submit_log.user_id contains non-numeric values; migration aborted to prevent data loss';
    end if;

    if exists (
        select 1 from information_schema.columns
        where table_schema = database()
          and table_name = 'submit_log'
          and column_name = 'user_id'
          and data_type <> 'bigint'
    ) then
        alter table submit_log
            modify column user_id bigint(20) not null comment '用户id';
    end if;
end$$
delimiter ;

call migrate_submit_log_user_id_type();
drop procedure migrate_submit_log_user_id_type;

delimiter $$
drop procedure if exists migrate_user_profile_indexes$$
create procedure migrate_user_profile_indexes()
begin
    if not exists (
        select 1 from information_schema.statistics
        where table_schema = database()
          and table_name = 'solution_explanation'
          and index_name = 'solution_explanation_user_create_idx'
    ) then
        alter table solution_explanation
            add index solution_explanation_user_create_idx(user_id, create_time, solution_id);
    end if;
end$$
delimiter ;

call migrate_user_profile_indexes();
drop procedure migrate_user_profile_indexes;
