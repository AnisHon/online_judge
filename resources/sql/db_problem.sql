-- ----------------------------
-- 题目服务的数据库
-- ----------------------------

create database db_problem character set utf8mb4;
use db_problem;


-- ----------------------------
-- 1、编程语言表
-- ----------------------------
drop table if exists sys_language;
CREATE TABLE sys_language (
    language_id     bigint(20)      not null auto_increment comment '主键',
    language_name   varchar(255)    default null            comment '语言名字',
    compile_command mediumtext                              comment '编译指令',
    seq             int(11)         default 0               comment '语言排序',
    gmt_create      datetime        default now()           comment '创建时间',
    gmt_modified    datetime        default now() on update now(),
    del_flag        boolean         default 0 not null      comment '删除标记',
    PRIMARY KEY (language_id)
) ENGINE=InnoDB auto_increment=1 default charset=utf8 comment '编程语言表';

insert into
    sys_language(language_name, compile_command, seq)
values
    ('C++', '/usr/bin/g++', 1),
    ('C++ With O2', '/usr/bin/g++', 2),
    ('C++ 17', '/usr/bin/g++', 3),
    ('C++ 17 With O2', '/usr/bin/g++', 4),
    ('C++ 20', '/usr/bin/g++', 5),
    ('C++ 20 With O2', '/usr/bin/g++', 6),
    ('C', '/usr/bin/g++', 7),
    ('C With O2', '/usr/bin/g++', 8),
    ('Java', '/usr/bin/javac', 9),
#     ('Python2', '/usr/bin/python', 10),
    ('Python3', '/usr/bin/python', 11),
    ('Golang', '/usr/bin/python', 12);


-- ----------------------------
-- 2、题目主表
-- ----------------------------
drop table if exists problem;
CREATE TABLE problem (
    problem_id      bigint(20)     not null auto_increment comment '主键',
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
) ENGINE=InnoDB auto_increment=1000 default charset=utf8 comment '题目主表，OJ题目有分表，非OJ不需要继续分表';;

-- ----------------------------
-- 3、OJ题目分表
-- ----------------------------
drop table if exists oj_problem;
CREATE TABLE oj_problem (
    problem_id      bigint(20)     not null auto_increment comment '主键',
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
        references problem(problem_id)
) ENGINE=InnoDB auto_increment=1000 default charset=utf8 comment 'OJ题目分表';

-- ----------------------------
-- 4、OJ题目测试用例表
-- ----------------------------
drop table if exists oj_problem_case;
CREATE TABLE oj_problem_case (
    case_id     bigint(20)  not null auto_increment comment '主键id',
    problem_id  bigint(20)  not null                comment '题目id',
    input       longtext                            comment '测试样例的输入',
    output      longtext                            comment '测试样例的输出',
    score       decimal(3, 2) default 0             comment '答对的分数',
    del_flag    boolean default 0 not null          comment '删除标记',
    create_time datetime default now(),
    update_time datetime default now() on update now(),
    PRIMARY KEY (case_id),
    constraint oj_problem_case_problem_id_fk foreign key oj_problem_case(problem_id)
                             references problem(problem_id)
) ENGINE=InnoDB auto_increment=1 default charset=utf8 comment 'OJ判题测试用例';

create index oj_problem_case_problem_id_idx on oj_problem_case(problem_id);


-- ----------------------------
-- 5、选择填空题答案表
-- ----------------------------
drop table if exists choice_fill_answers;
CREATE TABLE choice_fill_answers (
    answer_id   bigint(20)      not null auto_increment comment '主键id',
    problem_id bigint(20)       not null                comment '题目id',
    answer_text text            default null            comment '选项或填空答案',
    is_correct  bool            default false           comment '是否为正确答案（选择题专用）默认false',
    score       decimal(3, 2)   default 0 not null      comment '分数',
    blank_index int             null                    comment '填空题空格索引, 选择题ABCD索引 1表示A',
    del_flag    boolean         default 0 not null      comment '删除标记',
    create_time datetime        default now() not null ,
    update_time datetime        default now() on update now() not null ,
    PRIMARY KEY (answer_id),
    constraint choice_fill_answers_problem_id_fk foreign key choice_fill_answers(problem_id)
                                 references problem(problem_id)
)ENGINE=InnoDB auto_increment=1 default charset=utf8 comment '填空选择题答案表';
create index choice_fill_answers_problem_id_idx on choice_fill_answers(problem_id);


-- ----------------------------
-- 6、题单表
-- ----------------------------
drop table if exists problem_list;
CREATE TABLE problem_list (
    list_id     bigint(20)      not null auto_increment comment '主键',
    list_name   varchar(32)     unique                  comment '题单名字，必须唯一',
    description varchar(255)    not null                comment '题单说明，字数不应该太多',
    create_time datetime        default now() not null ,
    update_time datetime        default now() on update now() not null ,
    del_flag    boolean         default 0 not null      comment '删除标记',
    PRIMARY KEY (list_id)
) ENGINE=InnoDB auto_increment=1 default charset=utf8 comment '题单表';

-- ----------------------------
-- 7、题单 题目关系表
-- ----------------------------
drop table if exists problem_problem_list;
CREATE TABLE problem_problem_list (
    list_id         bigint(20)      not null            comment '单子id',
    problem_id      bigint(20)      not null            comment '题目id',
    problem_order   int(11)         not null            comment '题目顺序',
    score           decimal(4,2)    default 10 not null comment '每道题对应分数',
    primary key (list_id, problem_id),
    constraint problem_list_problem_id_fk foreign key problem_problem_list(problem_id)
        references problem(problem_id),
    constraint problem_list_list_id_fk foreign key problem_problem_list(list_id)
        references problem_list(list_id)
) ENGINE=InnoDB auto_increment=1 default charset=utf8 comment '题单 题目关系表';
create index problem_problem_list_list_id_idx on problem_problem_list(list_id);
create index problem_problem_list_problem_id_idx on problem_problem_list(problem_id);


-- ----------------------------
-- 8、比赛表
-- ----------------------------
drop table if exists contest;
CREATE TABLE contest (
    contest_id  bigint(20)      not null auto_increment,
    user_id     bigint(20)      not null                comment '比赛创建者id',
    title       varchar(255)    default null            comment '比赛标题',
    list_id     bigint(20)      not null                comment '题单id',
    description longtext        null                    comment '比赛说明',
    auth        int(11)         not null                comment '0公开赛，1为私有赛（访问需要密码）2为白名单模式',
    pwd         varchar(255)    default null            comment '比赛密码',
    start_time  datetime        default null            comment '开始时间',
    end_time    datetime        default null            comment '结束时间',
    del_flag    boolean         default 0  not null     comment '删除标记',
    create_time datetime        default now(),
    update_time datetime        default now() on update now(),
    primary key (contest_id),
    constraint contest_list_id_fk foreign key contest(list_id)
        references problem_list(list_id)
) ENGINE=InnoDB auto_increment=1000 default charset=utf8 comment '比赛表';


-- ----------------------------
-- 9、题目标签表
-- ----------------------------
drop table if exists tag;
CREATE TABLE tag (
    tag_id      bigint(20)      not null auto_increment comment '主键',
    tag_name    varchar(255)    unique                  comment '题目标签',
    tag_color   varchar(10)     not null                comment '颜色RGB值，带#',
    create_time datetime        default now() not null ,
    update_time datetime        default now() on update now(),
    del_flag    boolean         default 0 not null      comment '删除标记',
    PRIMARY KEY (tag_id)
) ENGINE=InnoDB auto_increment=1 default charset=utf8 comment '题目标签表';


-- ----------------------------
-- 10、题目标签 题目关系表
-- ----------------------------
drop table if exists problem_tag;
CREATE TABLE problem_tag (
    problem_id  bigint(20)      comment '题目id',
    tag_id      bigint(20)      comment '标签id',
    primary key (problem_id, tag_id),
    constraint problem_tag_problem_id_fk foreign key problem_tag(problem_id)
        references problem(problem_id),
    constraint problem_tag_tag_id_fk foreign key problem_tag(tag_id)
        references tag(tag_id)
)ENGINE=InnoDB auto_increment=1 default charset=utf8 comment '标签 题目关系表';
create index problem_tag_tag_id_idx on problem_tag(tag_id);

-- ----------------------------
-- 11、用户判题提交记录表
-- ----------------------------
drop table if exists submit_log;
CREATE TABLE submit_log (
    submit_id   bigint(20)  not null auto_increment comment '提交ID',
    user_id     varchar(32) not null                comment '用户id',
    problem_id  bigint(20)  not null                comment '题目id',
    language    varchar(20) not null                comment '使用语言的id',
    status      varchar(10) null                    comment '提交结果，取值范围 (AC, RE, WA, TLE, MLE, COMPILING QUEUE)',
    stderr      text        null                    comment '报错信息',
    time        int(20)     null                    comment '耗时 单位ms',
    memory      int(20)     null                    comment '内存使用 单位kb',
    submit_time datetime    default now() not null,
    PRIMARY KEY (submit_id),
    constraint submit_log_problem_id_fk foreign key submit_log(problem_id)
        references problem(problem_id)
) ENGINE=InnoDB auto_increment=1 default charset=utf8 comment 'OJ判题提交记录';
create index submit_log_problem_id on submit_log(problem_id);
create index submit_log_user_id on submit_log(user_id);
create index submit_log_user_problem_id on submit_log(problem_id, user_id);



-- ----------------------------
-- 12、题单文件夹表
-- ----------------------------
drop table if exists folder;
create table folder(
    folder_id   bigint(20)  not null auto_increment comment '文件夹ID，不存在ID为0的wjj',
    folder_name varchar(32) not null unique         comment '唯一文件夹名',
    folder_type char(1)     default 'M' not null    comment '类型(D directory 目录，F file 文件, M 菜单栏)',
    parent_id   bigint(20)  default 0 not null      comment '父文件夹名，默认0表示没有父文件夹',
    list_id     bigint(20)  null                    comment '题单，如果是D类型则应该为空',
    del_flag    boolean     default 0  not null     comment '逻辑删除',
    primary key (folder_id),
    constraint folder_list_id_fk foreign key folder(list_id)
        references problem_list(list_id)
) ENGINE=InnoDB auto_increment=1 default charset=utf8 comment '文件夹表';

-- ----------------------------
-- 13、题目完成表
-- ----------------------------
drop table if exists records;
create table records(
    record_id   bigint(20)      not null auto_increment,
    contest_id  bigint(20)      null                     comment '比赛ID，非比赛可不填，已废弃后续删除',
    user_id     bigint(20)      not null                 comment '用户ID',
    problem_id  bigint(20)      not null                 comment '题目id',
    status      boolean         not null                 comment '是否正确',
    score       DECIMAL(3, 2)   null                     comment '最终得分',
    answer      json            null                     comment '答案',
    primary key (record_id),
    constraint folder_contest_id_fk foreign key records(contest_id)
        references contest(contest_id),
    constraint folder_problem_id_fk foreign key records(problem_id)
        references problem(problem_id)
) ENGINE=InnoDB auto_increment=1 default charset=utf8 comment '题目完成表';
create index records_contest_id_idx on records(contest_id);
create index records_user_id_idx on records(user_id);
create index records_problem_id_idx on records(problem_id);
create index records_problem_id_user_id_idx on records(problem_id, user_id);




-- ----------------------------
-- 13、比赛完成表-2 后期预留表
-- ----------------------------
drop table if exists contest_records;
create table contest_records(
    record_id   bigint(20)      not null auto_increment,
    contest_id  bigint(20)      not null                 comment '比赛ID',
    user_id     bigint(20)      not null                 comment '用户ID',
    problem_id  bigint(20)      not null                 comment '题目id',
    status      boolean         not null                 comment '是否正确',
    score       DECIMAL(3, 2)   null                     comment '最终得分',
    primary key (record_id),
    unique key (record_id, user_id, problem_id),
    constraint contest_records_contest_id_fk foreign key contest_records(contest_id)
        references contest(contest_id),
    constraint contest_records_problem_id_fk foreign key contest_records(problem_id)
        references problem(problem_id)
) ENGINE=InnoDB auto_increment=1 default charset=utf8 comment '比赛题目记录';
create index contest_records_contest_id_idx on records(contest_id);
create index contest_records_user_id_idx on records(user_id);
create index contest_records_problem_id_idx on records(problem_id);

-- ----------------------------
-- 13、比赛完成记录表，分表专门用于存储答案-3 后期预留表
-- ----------------------------
drop table if exists contest_answer_records;
create table contest_answer_records(
    record_id   bigint(20)      not null auto_increment,
    answer      json            null                     comment '答案',
    primary key (record_id),
    constraint contest_answer_records_id_fk foreign key contest_answer_records(record_id)
    references contest_records(record_id)
) ENGINE=InnoDB auto_increment=1 default charset=utf8 comment '比赛完成记录表，分表专门用于存储答案';


-- ----------------------------
-- 13、题目完成表
-- ----------------------------
drop table if exists problem_complete;
create table problem_complete(
    user_id     bigint(20)      not null                 comment '用户ID',
    problem_id  bigint(20)      not null                 comment '题目id',
    primary key (user_id, problem_id),
    constraint problem_complete_problem_id_fk foreign key problem_complete(problem_id)
        references problem(problem_id)
) ENGINE=InnoDB auto_increment=1 default charset=utf8 comment '题目完成表';

-- ----------------------------
-- 14、比赛参加表
-- ----------------------------
drop table if exists user_contest;
create table user_contest(
    user_id bigint(20) not null comment '用户ID',
    contest_id bigint(20) not null comment '比赛ID',
    primary key (user_id, contest_id),
    constraint user_contest_contest_id_fk foreign key user_contest(contest_id)
        references contest(contest_id)
) ENGINE=InnoDB default charset=utf8 comment '比赛参加表';
create index user_contest_user_id_idx on user_contest(user_id);
create index user_contest_contest_id_idx on user_contest(contest_id);


-- ----------------------------
-- 15、白名单题单关系表
-- ----------------------------
drop table if exists class_contest;
create table class_contest(
    class_id bigint(20) not null comment '班级ID',
    contest_id bigint(20) not null comment '比赛ID',
    primary key (class_id, contest_id),
    constraint class_contest_contest_id_fk foreign key class_contest(contest_id)
        references contest(contest_id)
) ENGINE=InnoDB default charset=utf8 comment '白名单题单关系表';
create index class_contest_class_id_idx on class_contest(class_id);
create index class_contest_contest_id_idx on class_contest(contest_id);


-- ----------------------------
-- 16.题解表
-- ----------------------------
drop table if exists solution_explanation;
create table solution_explanation (
    solution_id bigint(20)      not null auto_increment comment '主键',
    title       varchar(255)    not null                comment '题解标题',
    problem_id  bigint(20)      not null                comment '对应题目',
    user_id     bigint(20)      not null                comment '发送者ID',
    top_up      boolean         not null default false  comment '是否置顶',
    private     boolean         not null default false  comment '是否尽自己可见',
    create_time datetime        not null default current_timestamp,
    update_time datetime        not null default current_timestamp,
    primary key solution_explanation(solution_id),
    constraint solution_explanation_problem_id_fk foreign key solution_explanation(problem_id)
        references problem(problem_id)
) ENGINE=InnoDB default charset=utf8 auto_increment=1 comment '文件信息表';

-- ----------------------------
-- 17.题解-内容表
-- ----------------------------
drop table if exists solution_explanation_content;
create table solution_explanation_content (
    solution_id bigint(20)      not null primary key    comment '主键',
    content     text            not null                comment '内容',
    constraint solution_explanation_content_id_fk foreign key solution_explanation_content(solution_id)
      references solution_explanation(solution_id) on delete cascade
) ENGINE=InnoDB default charset=utf8 comment '题解-内容表';
