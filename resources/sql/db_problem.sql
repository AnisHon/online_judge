-- ----------------------------
-- 题目服务的数据库
-- ----------------------------
drop database if exists db_problem;
create database db_problem character set utf8mb4;
use db_problem;


-- ----------------------------
-- 1、编程语言表
-- ----------------------------
drop table if exists sys_language;
CREATE TABLE sys_language (
    language_id     bigint(20)      not null                comment '主键',
    language_name   varchar(255)    default null            comment '语言名字',
    compile_command mediumtext                              comment '编译指令',
    seq             int(11)         default 0               comment '语言排序',
    gmt_create      datetime        default now()           comment '创建时间',
    gmt_modified    datetime        default now() on update now(),
    del_flag        boolean         default 0 not null      comment '删除标记',
    PRIMARY KEY (language_id)
) ENGINE=InnoDB default charset=utf8mb4 comment '编程语言表';

insert into
    sys_language(language_id, language_name, compile_command, seq)
values
    (1, 'C++', '/usr/bin/g++', 1),
    (2, 'C++ With O2', '/usr/bin/g++', 2),
    (3, 'C++ 17', '/usr/bin/g++', 3),
    (4, 'C++ 17 With O2', '/usr/bin/g++', 4),
    (5, 'C++ 20', '/usr/bin/g++', 5),
    (6, 'C++ 20 With O2', '/usr/bin/g++', 6),
    (7, 'C', '/usr/bin/g++', 7),
    (8, 'C With O2', '/usr/bin/g++', 8),
    (9, 'Java', '/usr/bin/javac', 9),
#     ('Python2', '/usr/bin/python', 10),
    (10, 'Python3', '/usr/bin/python', 11),
    (11, 'Golang', '/usr/bin/python', 12);


-- ----------------------------
-- 2、题目主表
-- ----------------------------
drop table if exists problem;
CREATE TABLE problem (
    problem_id      bigint(20)     not null                comment '主键',
    oj_id           bigint(20)     null                    comment 'oj题目ID',
    title           varchar(255)   not null                comment '题目名称',
    type            int(11)        default 1               comment '题目类型，(1 OJ, 2 FILL, 3 CHOICE, 4 MULTI_CHOICE)',
    source          varchar(255)   default '公有题库'       comment '题目来源',
    description     longtext       not null                comment '题目描述，图片放在这里吧',
    hint            longtext       default null            comment '备注,提醒',
    auth            int(11)        default '1'             comment '默认为1公开，2为比赛题目',
    del_flag        boolean        default 0 not null      comment '删除标记(0未删除 1删除)',
    create_time     datetime       default now()           comment '创建时间',
    update_time     datetime       default now()           comment '更新时间，用于乐观锁',
    PRIMARY KEY (problem_id)
) ENGINE=InnoDB default charset=utf8mb4 comment '题目主表，OJ题目有分表，非OJ不需要继续分表';;

-- ----------------------------
-- 3、OJ题目分表
-- ----------------------------
drop table if exists oj_problem;
CREATE TABLE oj_problem (
    problem_id      bigint(20)     not null                 comment '主键',
    time_limit      int(11)        default 1000            comment '单位ms',
    difficulty      int(11)        default 0               comment '难度 (0 未分类, 1 简单, 2 中等, 3 困难)',
    memory_limit    int(11)        default 65535           comment '单位kb',
    stack_limit     int(11)        default 128             comment '单位mb',
    input           longtext       not null                comment '输入描述',
    output          longtext       not null                comment '输出描述',
    input_example   longtext       not null                comment '输入样例',
    output_example  longtext       not null                comment '输出样例',
    del_flag        boolean        default 0 not null      comment '删除标记(0未删除 1删除)',
    create_time     datetime       default now()           comment '创建时间',
    update_time     datetime       default now()           comment '更新时间，用于乐观锁',
    PRIMARY KEY (problem_id),
    constraint oj_problem_problem_id_fk foreign key oj_problem(problem_id)
        references problem(problem_id) on delete cascade
) ENGINE=InnoDB default charset=utf8mb4 comment 'OJ题目分表';

-- ----------------------------
-- 4、OJ题目测试用例表
-- ----------------------------
drop table if exists oj_problem_case;
CREATE TABLE oj_problem_case (
    case_id     bigint(20)      not null                comment '主键id',
    problem_id  bigint(20)      not null                comment '题目id',
    input       longtext                            comment '测试样例的输入',
    output      longtext                            comment '测试样例的输出',
    score       decimal(10, 3)  default 1.00          comment '答对的分数',
    del_flag    boolean default 0 not null          comment '删除标记',
    create_time datetime default now(),
    update_time datetime default now() on update now(),
    PRIMARY KEY (case_id),
    constraint oj_problem_case_problem_id_fk foreign key oj_problem_case(problem_id)
        references problem(problem_id) on delete cascade
) ENGINE=InnoDB default charset=utf8mb4 comment 'OJ判题测试用例';

create index oj_problem_case_problem_id_idx on oj_problem_case(problem_id);


-- ----------------------------
-- 5、选择填空题答案表
-- ----------------------------
drop table if exists choice_fill_answers;
CREATE TABLE choice_fill_answers (
    answer_id   bigint(20)      not null                comment '主键id',
    problem_id bigint(20)       not null                comment '题目id',
    answer_text text            default null            comment '选项或填空答案',
    is_correct  bool            default false           comment '是否为正确答案（选择题专用）默认false',
    score       decimal(10, 3)  default 1 not null      comment '分数',
    blank_index int             null                    comment '填空题空格索引, 选择题ABCD索引 1表示A',
    del_flag    boolean         default 0 not null      comment '删除标记',
    create_time datetime        default now() not null ,
    update_time datetime        default now() on update now() not null ,
    PRIMARY KEY (answer_id),
    constraint choice_fill_answers_problem_id_fk foreign key choice_fill_answers(problem_id)
        references problem(problem_id) on delete cascade
)ENGINE=InnoDB default charset=utf8mb4 comment '填空选择题答案表';
create index choice_fill_answers_problem_id_idx on choice_fill_answers(problem_id);


-- ----------------------------
-- 6、题单表
-- ----------------------------
drop table if exists problem_list;
CREATE TABLE problem_list (
    list_id     bigint(20)      not null                comment '主键',
    list_name   varchar(32)     unique                  comment '题单名字，必须唯一',
    description varchar(255)    not null                comment '题单说明，字数不应该太多',
    create_time datetime        default now() not null ,
    update_time datetime        default now() on update now() not null ,
    del_flag    boolean         default 0 not null      comment '删除标记',
    PRIMARY KEY (list_id)
) ENGINE=InnoDB default charset=utf8mb4 comment '题单表';

-- ----------------------------
-- 7、题单 题目关系表
-- ----------------------------
drop table if exists problem_problem_list;
CREATE TABLE problem_problem_list (
    list_id         bigint(20)      not null            comment '单子id',
    problem_id      bigint(20)      not null            comment '题目id',
    problem_order   int(11)         not null            comment '题目顺序',
    score           decimal(10,3)   default 10 not null comment '每道题对应分数',
    primary key (list_id, problem_id),
    constraint problem_list_problem_id_fk foreign key problem_problem_list(problem_id)
        references problem(problem_id) on delete cascade,
    constraint problem_list_list_id_fk foreign key problem_problem_list(list_id)
        references problem_list(list_id) on delete cascade
) ENGINE=InnoDB default charset=utf8mb4 comment '题单 题目关系表';
create index problem_problem_list_list_id_idx on problem_problem_list(list_id);
create index problem_problem_list_problem_id_idx on problem_problem_list(problem_id);


-- ----------------------------
-- 8、比赛表
-- ----------------------------
drop table if exists contest;
CREATE TABLE contest (
    contest_id  bigint(20)      not null,
    user_id     bigint(20)      not null                comment '比赛创建者id',
    title       varchar(255)    default null            comment '比赛标题',
    list_id     bigint(20)      not null                comment '题单id',
    description longtext        null                    comment '比赛说明',
    auth        int(11)         not null                comment '0公开赛，1为私有赛（访问需要密码）2为白名单模式',
    type        int             not null  default 0     comment '类型(0 比赛, 1 作业)',
    pwd         varchar(255)    default null            comment '比赛密码',
    start_time  datetime        default null            comment '开始时间',
    end_time    datetime        default null            comment '结束时间',
    scoring_version int         not null default 2      comment '1=legacy projection, 2=versioned scoring',
    next_attempt_seq bigint(20) not null default 0      comment 'Highest accepted formal attempt sequence',
    problem_snapshot_at datetime(3) null                comment '题目集合冻结时间',
    submitted   boolean         not null default 0      comment '是否提交，比赛模式提交后不能修改',
    del_flag    boolean         not null default 0      comment '删除标记',
    create_time datetime        default now(),
    update_time datetime        default now() on update now(),
    primary key (contest_id),
    constraint contest_list_id_fk foreign key contest(list_id)
        references problem_list(list_id) on delete restrict on update restrict
) ENGINE=InnoDB default charset=utf8mb4 comment '比赛表';


-- ----------------------------
-- 9、题目标签表
-- ----------------------------
drop table if exists tag;
CREATE TABLE tag (
    tag_id      bigint(20)      not null                comment '主键',
    tag_name    varchar(255)    unique                  comment '题目标签',
    tag_color   varchar(10)     not null                comment '颜色RGB值，带#',
    create_time datetime        default now() not null ,
    update_time datetime        default now() on update now(),
    del_flag    boolean         default 0 not null      comment '删除标记',
    PRIMARY KEY (tag_id)
) ENGINE=InnoDB default charset=utf8mb4 comment '题目标签表';


-- ----------------------------
-- 10、题目标签 题目关系表
-- ----------------------------
drop table if exists problem_tag;
CREATE TABLE problem_tag (
    problem_id  bigint(20)      comment '题目id',
    tag_id      bigint(20)      comment '标签id',
    primary key (problem_id, tag_id),
    constraint problem_tag_problem_id_fk foreign key problem_tag(problem_id)
        references problem(problem_id) on delete cascade,
    constraint problem_tag_tag_id_fk foreign key problem_tag(tag_id)
        references tag(tag_id) on delete cascade
)ENGINE=InnoDB default charset=utf8mb4 comment '标签 题目关系表';
create index problem_tag_tag_id_idx on problem_tag(tag_id);

-- ----------------------------
-- 11、用户判题提交记录表
-- ----------------------------
drop table if exists submit_log;
CREATE TABLE submit_log (
    submit_id   bigint(20)  not null                comment '提交ID',
    user_id     bigint(20)   not null                comment '用户id',
    problem_id  bigint(20)  not null                comment '题目id',
    language    varchar(20) not null                comment '使用语言的id',
    contest_id  bigint(20)  null                    comment '比赛ID',
    code        longtext    null                    comment '用户提交的源代码',
    status      varchar(32) null                    comment '公开判题状态',
    stderr      text        null                    comment '报错信息',
    internal_error longtext null                    comment '判题机内部错误，仅管理侧使用',
    error_code  varchar(64) null                    comment '判题机内部错误码',
    time        int(20)     null                    comment '耗时 单位ms',
    memory      int(20)     null                    comment '内存使用 单位kb',
    total_count int         null                    comment '测试用例总数',
    pass_count  int         null                    comment '通过测试用例数',
    score       decimal(10, 2) null                  comment '判题最终得分',
    result_applied boolean not null default 0       comment '结果是否已应用到比赛成绩投影',
    completed_at datetime(3) null                   comment '真实判题完成时间，历史不补造',
    submit_time datetime    default now() not null,
    PRIMARY KEY (submit_id),
    constraint submit_log_problem_id_fk foreign key submit_log(problem_id)
        references problem(problem_id) on delete cascade
) ENGINE=InnoDB default charset=utf8mb4 comment 'OJ判题提交记录';
create index submit_log_problem_id on submit_log(problem_id);
create index submit_log_user_id on submit_log(user_id);
create index submit_log_user_submit_time on submit_log(user_id, submit_time);
create index submit_log_user_problem_id on submit_log(problem_id, user_id);
create index idx_submit_log_contest_status_time_id on submit_log(contest_id, status, submit_time, submit_id);

-- ----------------------------
-- 11.1、判题机内部测试用例日志
-- ----------------------------
create table judge_case_log (
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
    key judge_case_log_problem_id_idx (problem_id),
    unique key uk_judge_case_log_submit_case (submit_id, case_index)
) engine=innodb default charset=utf8mb4 comment 'OJ判题机内部测试用例日志';



-- ----------------------------
-- 12、题单文件夹表
-- ----------------------------
drop table if exists folder;
create table folder(
    folder_id   bigint(20)  not null                comment '文件夹ID，不存在ID为0的wjj',
    folder_name varchar(32) not null unique         comment '唯一文件夹名',
    folder_type char(1)     default 'M' not null    comment '类型(D directory 目录，F file 文件, M 菜单栏)',
    parent_id   bigint(20)  default 0 not null      comment '父文件夹名，默认0表示没有父文件夹',
    order_      int         default 0 not null      comment '文件夹顺序',
    list_id     bigint(20)  null                    comment '题单，如果是D类型则应该为空',
    del_flag    boolean     default 0  not null     comment '逻辑删除',
    primary key (folder_id),
    constraint folder_list_id_fk foreign key folder(list_id)
        references problem_list(list_id) on delete cascade
) ENGINE=InnoDB default charset=utf8mb4 comment '文件夹表';

-- ----------------------------
-- 13、题目完成表
-- ----------------------------
drop table if exists records;
create table records(
    record_id   bigint(20)      not null ,
    contest_id  bigint(20)      null                     comment '比赛ID，非比赛可不填，已废弃后续删除',
    user_id     bigint(20)      not null                 comment '用户ID',
    problem_id  bigint(20)      not null                 comment '题目id',
    status      boolean         not null default 0       comment '是否正确',
    score       DECIMAL(10, 3)  null                     comment '最终得分',
    answer      json            null                     comment '答案',
    primary key (record_id),
    constraint folder_contest_id_fk foreign key records(contest_id)
        references contest(contest_id) on delete cascade,
    constraint folder_problem_id_fk foreign key records(problem_id)
        references problem(problem_id) on delete cascade
) ENGINE=InnoDB default charset=utf8mb4 comment '题目完成表';
create index records_contest_id_idx on records(contest_id);
create index records_user_id_idx on records(user_id);
create index records_problem_id_idx on records(problem_id);
create index records_problem_id_user_id_idx on records(problem_id, user_id);




-- ----------------------------
-- 13、比赛完成表-2 后期预留表
-- ----------------------------
drop table if exists contest_records;
create table contest_records(
    record_id   bigint(20)      not null ,
    contest_id  bigint(20)      not null                 comment '比赛ID',
    user_id     bigint(20)      not null                 comment '用户ID',
    problem_id  bigint(20)      not null                 comment '题目id',
    status      boolean         not null default 0       comment '是否正确',
    score       DECIMAL(10, 3)  null                     comment '最终得分',
    applied_attempt_seq bigint(20) not null default 0    comment '已应用的正式提交序号',
    applied_attempt_id bigint(20) null                  comment '当前成绩来源attempt',
    primary key (record_id),
    unique key (contest_id, user_id, problem_id),
    constraint contest_records_contest_id_fk foreign key contest_records(contest_id)
        references contest(contest_id) on delete restrict on update restrict,
    constraint contest_records_problem_id_fk foreign key contest_records(problem_id)
        references problem(problem_id) on delete restrict on update restrict
) ENGINE=InnoDB default charset=utf8mb4 comment '比赛题目记录';
create index contest_records_contest_id_idx on contest_records(contest_id);
create index contest_records_user_id_idx on contest_records(user_id);
create index contest_records_problem_id_idx on contest_records(problem_id);

-- ----------------------------
-- 13.1、比赛题目快照、正式尝试和榜单版本结构
-- ----------------------------
create table contest_problem_snapshot (
    contest_id bigint(20) not null,
    problem_id bigint(20) not null,
    problem_order int not null,
    title varchar(255) not null,
    problem_type int not null,
    max_score decimal(10, 3) not null,
    created_at datetime(3) not null,
    primary key (contest_id, problem_id),
    unique key uk_contest_problem_snapshot_order (contest_id, problem_order),
    constraint fk_contest_problem_snapshot_contest foreign key (contest_id)
        references contest(contest_id) on delete restrict on update restrict,
    constraint chk_contest_problem_snapshot_values check (problem_order > 0 and max_score >= 0)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
  comment='冻结比赛题目元信息，不复制题目正文和测试用例';

create table contest_attempt (
    attempt_id bigint(20) not null comment 'ASSIGN_ID正式提交ID',
    contest_id bigint(20) not null,
    user_id bigint(20) not null,
    problem_id bigint(20) not null,
    attempt_seq bigint(20) not null,
    submit_id bigint(20) null,
    kind varchar(16) not null,
    accepted_at datetime(3) not null,
    state varchar(16) not null default 'PENDING',
    judge_status varchar(32) null,
    score decimal(10, 2) null,
    correct boolean null,
    completed_at datetime(3) null,
    primary key (attempt_id),
    unique key uk_contest_attempt_sequence (contest_id, attempt_seq),
    unique key uk_contest_attempt_submit (submit_id),
    key idx_contest_attempt_state_seq (contest_id, state, attempt_seq),
    key idx_contest_attempt_user_problem_seq (contest_id, user_id, problem_id, attempt_seq),
    constraint chk_contest_attempt_state check (state in ('PENDING', 'COMPLETED', 'INFRA_ERROR')),
    constraint chk_contest_attempt_score check (score is null or score >= 0)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
  comment='比赛正式提交的持久化序号和结果状态';

create table contest_rank_snapshot (
    contest_id bigint(20) not null,
    state varchar(16) not null default 'WAITING',
    version bigint(20) not null default 0,
    next_version bigint(20) not null default 0,
    build_attempts int not null default 0,
    next_build_at datetime(3) not null,
    source_seq bigint(20) not null default 0,
    source_mode varchar(24) not null,
    rule_version varchar(24) not null,
    total_users bigint(20) not null default 0,
    pending_count bigint(20) not null default 0,
    lease_owner varchar(64) null,
    lease_until datetime(3) null,
    generated_at datetime(3) null,
    last_error varchar(1000) null,
    updated_at datetime(3) not null,
    primary key (contest_id),
    key idx_contest_rank_snapshot_due (state, next_build_at, lease_until),
    constraint fk_contest_rank_snapshot_contest foreign key (contest_id)
        references contest(contest_id) on delete restrict on update restrict,
    constraint chk_contest_rank_snapshot_state check (state in ('WAITING', 'BUILDING', 'READY', 'ERROR')),
    constraint chk_contest_rank_snapshot_counts check (
        version >= 0 and next_version >= 0 and build_attempts >= 0
        and source_seq >= 0 and total_users >= 0 and pending_count >= 0
    )
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
  comment='最终榜单版本/租约状态';

create table contest_rank_entry (
    contest_id bigint(20) not null,
    version bigint(20) not null,
    user_id bigint(20) not null,
    rank_no bigint(20) not null,
    row_position bigint(20) not null,
    score decimal(18, 2) not null,
    correct_count int not null default 0,
    answered_count int not null default 0,
    handed_in boolean not null default 0,
    created_at datetime(3) not null,
    primary key (contest_id, version, user_id),
    unique key uk_contest_rank_entry_position (contest_id, version, row_position),
    key idx_contest_rank_entry_page (contest_id, version, rank_no, row_position),
    constraint chk_contest_rank_entry_values check (
        version > 0 and rank_no > 0 and row_position > 0 and score >= 0
        and correct_count >= 0 and answered_count >= 0
    )
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
  comment='最终榜单某一版本的用户聚合行';

-- ----------------------------
-- 13、比赛完成记录表，分表专门用于存储答案-3 后期预留表
-- ----------------------------
drop table if exists contest_answer_records;
create table contest_answer_records(
    record_id   bigint(20)      not null,
    answer      json            null                     comment '答案',
    primary key (record_id),
    constraint contest_answer_records_id_fk foreign key contest_answer_records(record_id)
    references contest_records(record_id) on delete cascade
) ENGINE=InnoDB default charset=utf8mb4 comment '比赛完成记录表，分表专门用于存储答案';


-- ----------------------------
-- 13、题目完成表
-- ----------------------------
drop table if exists problem_complete;
create table problem_complete(
    user_id     bigint(20)      not null                 comment '用户ID',
    problem_id  bigint(20)      not null                 comment '题目id',
    primary key (user_id, problem_id),
    constraint problem_complete_problem_id_fk foreign key problem_complete(problem_id)
        references problem(problem_id) on delete cascade
) ENGINE=InnoDB default charset=utf8mb4 comment '题目完成表';

-- ----------------------------
-- 14、比赛参加表
-- ----------------------------
drop table if exists user_contest;
create table user_contest(
    user_id bigint(20) not null comment '用户ID',
    contest_id bigint(20) not null comment '比赛ID',
    primary key (user_id, contest_id),
    constraint user_contest_contest_id_fk foreign key user_contest(contest_id)
        references contest(contest_id) on delete cascade
) ENGINE=InnoDB default charset=utf8mb4 comment '比赛参加表';
create index user_contest_user_id_idx on user_contest(user_id);
create index user_contest_contest_id_idx on user_contest(contest_id);


-- ----------------------------
-- 15、用户提交表
-- ----------------------------
drop table if exists user_submit;
create table user_submit(
    user_id     bigint(20) not null                 comment '用户ID',
    contest_id  bigint(20) not null                 comment '比赛ID',
    submit_time datetime   not null default now()   comment '提交时间',
    primary key (user_id, contest_id),
    constraint user_submit_contest_id_fk foreign key user_submit(contest_id)
        references contest(contest_id) on delete cascade
) ENGINE=InnoDB default charset=utf8mb4 comment '用户提交表';

-- ----------------------------
-- 15、用户补交表
-- ----------------------------
drop table if exists supplement_contest;
create table supplement_contest(
    user_id     bigint(20) not null                 comment '用户ID',
    contest_id  bigint(20) not null                 comment '比赛ID',
    deadline    datetime   not null default now()   comment '最迟提交时间，比赛不能补交',
    primary key (user_id, contest_id),
    constraint supplement_contest_contest_id_fk foreign key user_submit(contest_id)
        references contest(contest_id) on delete cascade
);

-- ----------------------------
-- 16. problem-service transactional outbox (points and judge dispatch only)
-- ----------------------------
create table problem_event_outbox (
    event_id char(36) character set ascii collate ascii_bin not null,
    dedupe_key varchar(160) character set ascii collate ascii_bin not null,
    event_type varchar(48) not null,
    exchange_name varchar(100) not null,
    routing_key varchar(100) not null,
    payload json not null,
    status varchar(16) not null default 'PENDING',
    attempts int not null default 0,
    next_attempt_at datetime(3) not null,
    lease_owner varchar(64) null,
    lease_until datetime(3) null,
    last_error varchar(1000) null,
    created_at datetime(3) not null,
    sent_at datetime(3) null,
    primary key (event_id),
    unique key uk_problem_event_outbox_dedupe (dedupe_key),
    key idx_problem_event_outbox_due (status, next_attempt_at, lease_until),
    constraint chk_problem_event_outbox_status check (status in ('PENDING', 'SENDING', 'SENT', 'FAILED')),
    constraint chk_problem_event_outbox_attempts check (attempts >= 0)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
  comment='Transactional problem-service message outbox';
