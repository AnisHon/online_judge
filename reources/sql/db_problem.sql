drop database if exists db_problem;
create database if not exists db_problem;
use db_problem;


-- ----------------------------
-- 1、编程语言表
-- ----------------------------
drop table if exists sys_language;
CREATE TABLE sys_language (
    language_id bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    language_name varchar(255) DEFAULT NULL COMMENT '语言名字',
    compile_command mediumtext COMMENT '编译指令',
    seq int(11) DEFAULT '0' COMMENT '语言排序',
    gmt_create datetime DEFAULT now() comment '创建时间',
    gmt_modified datetime DEFAULT now() ON UPDATE now(),
    del_flag boolean default 0 comment '删除标记',
    PRIMARY KEY (language_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 comment '编程语言表';

-- ----------------------------
-- 2、题目类型信息表
-- ----------------------------
drop table if exists sys_problem_type;
create table sys_problem_type(
    type_id     bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '类型id',
    type_name   varchar(32) NOT NULL UNIQUE         COMMENT '类型名',
    create_time datetime    DEFAULT now()           COMMENT '创建时间',
    update_time datetime    DEFAULT now()           COMMENT '更新时间，用于乐观锁',
    del_flag    boolean     DEFAULT 0               COMMENT '逻辑删除',
    primary key (type_id)
)ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8 COMMENT '题目类型信息表';

insert into
    sys_problem_type(type_name)
values
    ('OJ题目'),
    ('多选题'),
    ('单选题');


-- ----------------------------
-- 2、题目信息表
-- ----------------------------
drop table if exists sys_problem;
CREATE TABLE sys_problem (
    problem_id      bigint(20)     NOT NULL AUTO_INCREMENT COMMENT '主键',
    title           varchar(255)   NOT NULL                COMMENT '题目名称',
    author          varchar(255)   DEFAULT '未知'           COMMENT '作者',
    type_id         bigint(20)     DEFAULT 0               COMMENT '类型ID，如果不是0就用answer属性',
    time_limit      int(11)        DEFAULT '1000'          COMMENT '单位ms',
    memory_limit    int(11)        DEFAULT '65535'         COMMENT '单位kb',
    stack_limit     int(11)        DEFAULT '128'           COMMENT '单位mb',
    description     longtext       NOT NULL                COMMENT '题目描述',
    input           longtext       NOT NULL                COMMENT '输入描述',
    output          longtext       NOT NULL                COMMENT '输出描述',
    input_example   longtext       NOT NULL                COMMENT '输入样例',
    output_example  longtext       NOT NULL                COMMENT '输出样例',
    source          varchar(255)   DEFAULT '公有题库'       COMMENT '题目来源',
    hint            longtext       DEFAULT NULL            COMMENT '备注,提醒',
    auth            int(11)        DEFAULT '1'             COMMENT '默认为1公开，2为比赛题目',
    del_flag        boolean        DEFAULT 0               COMMENT  '删除标记(0未删除 1删除)',
    create_time     datetime       DEFAULT now()           COMMENT '创建时间',
    update_time     datetime       DEFAULT now()           COMMENT '更新时间，用于乐观锁',
    PRIMARY KEY (problem_id)
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8 COMMENT '题目信息';

-- ----------------------------
-- 3、题目测试用例表
-- ----------------------------
drop table if exists problem_case;
CREATE TABLE problem_case (
    case_id            bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键id',
    problem_id    bigint(20)  NOT NULL COMMENT '题目id',
    input         longtext COMMENT '测试样例的输入',
    output        longtext COMMENT '测试样例的输出',
    answer        json COMMENT '当类型是其他的时候，这个就是题目答案',
    score         int(11) DEFAULT 100 comment '答对的分数',
    del_flag boolean default 0 comment '删除标记',
    create_time   datetime DEFAULT now(),
    update_time   datetime DEFAULT now() ON UPDATE now(),
    PRIMARY KEY (case_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8  COMMENT '判题测试用例';

-- ----------------------------
-- 4、题单表
-- ----------------------------
drop table if exists sys_list;
CREATE TABLE sys_list (
    list_id bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    list_name varchar(32) unique comment '题单名字，必须唯一',
    description varchar(255) not null comment '题单说明，字数不应该太多',
    changes     int unsigned DEFAULT '4294967295' comment '提交次数限制',
    create_time datetime DEFAULT now(),
    update_time datetime DEFAULT now() ON UPDATE now(),
    del_flag boolean comment '删除标记',
    PRIMARY KEY (list_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 comment '题单表';

-- ----------------------------
-- 5、题单 题目关系表
-- ----------------------------
drop table if exists problem_list;
CREATE TABLE problem_list (
    list_id bigint(20)    comment '单子id',
    problem_id bigint(20) comment '题目id',
    problem_order int(11) comment '题目顺序',
    primary key (list_id, problem_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 comment '题单 题目关系表';



-- ----------------------------
-- 6、比赛表
-- ----------------------------
drop table if exists sys_contest;
CREATE TABLE sys_contest (
    contest_id bigint(20)  NOT NULL AUTO_INCREMENT,
    user_id bigint(20) NOT NULL COMMENT '比赛创建者id',
    title varchar(255) DEFAULT NULL COMMENT '比赛标题',
    list_id bigint(20) not null comment '题单',
    description longtext COMMENT '比赛说明',
    auth int(11) NOT NULL COMMENT '0公开赛，1为私有赛（访问需要密码）2为白名单模式',
    pwd varchar(255) DEFAULT NULL COMMENT '比赛密码',
    start_time datetime DEFAULT NULL COMMENT '开始时间',
    end_time datetime DEFAULT NULL COMMENT '结束时间',
    del_flag boolean default 0 comment '删除标记',
    create_time datetime DEFAULT now(),
    update_time datetime DEFAULT now() ON UPDATE now(),
    primary key (contest_id)
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8 comment '比赛表';




-- ----------------------------
-- 7、题目标签表
-- ----------------------------
drop table if exists sys_tag;
CREATE TABLE sys_tag (
    tag_id bigint(20)  NOT NULL AUTO_INCREMENT comment '主键',
    tag_name varchar(255) unique  comment '题目标签',
    tag_color varchar(10) comment '颜色RGB值，带#',
    create_time datetime DEFAULT now(),
    update_time datetime DEFAULT now() ON UPDATE now(),
    del_flag boolean comment '删除标记',
    PRIMARY KEY (tag_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT '题目标签表';


-- ----------------------------
-- 8、题目标签 题目关系表
-- ----------------------------
drop table if exists problem_tag;
CREATE TABLE problem_tag (
    problem_id bigint(20)  comment '题目id',
    tag_id bigint(20)      comment '标签id',
    primary key (problem_id, tag_id)
)ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 comment '标签 题目关系表';


-- ----------------------------
-- 9、用户提交OJ题目表
-- ----------------------------
drop table if exists submit_record;
CREATE TABLE submit_record (
    submit_id bigint(20) NOT NULL AUTO_INCREMENT comment '提交ID',
    user_id varchar(32) NOT NULL COMMENT '用户id',
    problem_id bigint(20)  NOT NULL COMMENT '题目id',
    language_id bigint(20) NOT NULL COMMENT '使用语言的id',
    result varchar(10) NULL COMMENT '结果，取值范围 (AC, RE, WA, TLE, MLE)',
    time int(11) NULL COMMENT '耗时 单位ms',
    memory int(11) NULL COMMENT '内存使用 单位kb',
    submit_time datetime DEFAULT now(),
    PRIMARY KEY (submit_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 comment '用户提交记录';

-- ----------------------------
-- 10、题单文件夹表
-- ----------------------------
drop table if exists sys_folder;
create table sys_folder(
    folder_id bigint(20) not null auto_increment comment '文件夹ID，不存在ID为0的wjj',
    folder_name varchar(32) not null unique comment '唯一文件夹名',
    folder_type char(1) default 'D' comment '类型(D directory 目录，F file 文件)',
    parent_id bigint(20) default 0 comment '父文件夹名，默认0表示没有父文件夹',
    list_id bigint(20) default null comment '题单，如果是D类型则应该为空',
    del_flag boolean default 0 comment 'logic delete',
    primary key (folder_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 comment '文件夹表';

-- ----------------------------
-- 10、题目单完成表
-- ----------------------------
drop table if exists finish_list;
create table finish_list(
    list_id bigint(20) not null comment '题单ID',
    user_id bigint(20) not null comment '用户ID',
    answer_id bigint(20) not null comment '题目id',
    status char(1) not null comment '最后结果(R 全对, W 全错, B 有对有错)',
    answer json not null comment '答案',
    primary key (list_id, user_id, answer_id)
)




