-- OJ 判题链路迁移：公开提交记录 + 内部测试用例日志。
-- 该脚本只新增字段/表和权限，不删除线上历史提交。
use db_problem;

alter table submit_log modify column status varchar(32) null comment '公开判题状态';

-- MySQL 8.0.27 不保证 ALTER TABLE ADD COLUMN IF NOT EXISTS，使用元数据检查保证重复执行安全。
delimiter $$
drop procedure if exists migrate_oj_judge_columns$$
create procedure migrate_oj_judge_columns()
begin
    if not exists (select 1 from information_schema.columns where table_schema = database() and table_name = 'submit_log' and column_name = 'contest_id') then
        alter table submit_log add column contest_id bigint(20) null comment '比赛ID';
    end if;
    if not exists (select 1 from information_schema.columns where table_schema = database() and table_name = 'submit_log' and column_name = 'code') then
        alter table submit_log add column code longtext null comment '用户提交的源代码';
    end if;
    if not exists (select 1 from information_schema.columns where table_schema = database() and table_name = 'submit_log' and column_name = 'internal_error') then
        alter table submit_log add column internal_error longtext null comment '判题机内部错误，仅管理侧使用';
    end if;
    if not exists (select 1 from information_schema.columns where table_schema = database() and table_name = 'submit_log' and column_name = 'error_code') then
        alter table submit_log add column error_code varchar(64) null comment '判题机内部错误码';
    end if;
    if not exists (select 1 from information_schema.columns where table_schema = database() and table_name = 'submit_log' and column_name = 'total_count') then
        alter table submit_log add column total_count int null comment '测试用例总数';
    end if;
    if not exists (select 1 from information_schema.columns where table_schema = database() and table_name = 'submit_log' and column_name = 'pass_count') then
        alter table submit_log add column pass_count int null comment '通过测试用例数';
    end if;
end$$
delimiter ;
call migrate_oj_judge_columns();
drop procedure migrate_oj_judge_columns;

create table if not exists judge_case_log (
    id              bigint(20) not null comment '日志ID',
    submit_id       bigint(20) not null comment '提交ID',
    problem_id      bigint(20) not null comment '题目ID',
    case_id         bigint(20) null comment '测试用例ID，兜底日志可为空',
    case_index      int not null comment '测试用例顺序',
    status          varchar(32) not null comment '单个测试用例状态',
    score           decimal(10, 3) default 0 comment '该测试用例得分',
    time            int(20) default 0 comment '耗时，单位ms',
    memory          int(20) default 0 comment '内存，单位kb',
    internal_error  longtext null comment '判题机内部错误，不对用户公开',
    create_time     datetime default now() not null,
    update_time     datetime default now() on update now() not null,
    primary key (id),
    key judge_case_log_submit_id_idx (submit_id),
    key judge_case_log_problem_id_idx (problem_id)
) engine=innodb default charset=utf8mb4 comment 'OJ判题机内部测试用例日志';

use db_user;

insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon)
values (150, '查看本人提交', 7, 10, '#', 'B', 'problem:submit:read', '#')
on duplicate key update menu_name = values(menu_name), perms = values(perms);

insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon)
values (151, '查看判题内部日志', 8, 10, '#', 'B', 'problem:judge:case:read', '#')
on duplicate key update menu_name = values(menu_name), perms = values(perms);

insert ignore into sys_role_menu(role_id, menu_id) values
    (1, 150), (2, 150), (3, 150), (4, 150),
    (3, 151), (4, 151);
