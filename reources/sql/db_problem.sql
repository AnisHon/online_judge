create database if not exists db_problem;
use db_problem;


-- ----------------------------
-- 1、题目信息表
-- ----------------------------
drop table if exists problem;
CREATE TABLE `problem` (
    `id`              bigint(20)     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `problem_id`      varchar(255)                           COMMENT '问题的自定义ID 例如（HOJ-1000）',
    `title`           varchar(255)   NOT NULL                COMMENT '题目名称',
    `author`          varchar(255)   DEFAULT '未知'           COMMENT '作者',
    `time_limit`      int(11)        DEFAULT '1000'          COMMENT '单位ms',
    `memory_limit`    int(11)        DEFAULT '65535'         COMMENT '单位kb',
    `stack_limit`     int(11)        DEFAULT '128'           COMMENT '单位mb',
    `description`     longtext       NOT NULL                COMMENT '题目描述',
    `input`           longtext       NOT NULL                COMMENT '输入描述',
    `output`          longtext       NOT NULL                COMMENT '输出描述',
    `input_example`   longtext       NOT NULL                COMMENT '输入样例',
    `output_example`  longtext       NOT NULL                COMMENT '输出样例',
    `source`          varchar(255)   DEFAULT '公有题库'       COMMENT '题目来源',
    `hint`            longtext       DEFAULT NULL            COMMENT '备注,提醒',
    `auth`            int(11)        DEFAULT '1'             COMMENT '默认为1公开，2为私有，3为比赛题目',
    `io_score`        int(11)        DEFAULT '100'           COMMENT '当该题目为io题目时的分数',
    `judge_mode`      varchar(255)   DEFAULT 'default'       COMMENT '题目评测模式,default、spj、interactive',
    `judge_case_mode` varchar(255)   DEFAULT 'default'       COMMENT '题目样例评测模式,default,subtask_lowest,subtask_average',
    `user_extra_file` mediumtext     DEFAULT NULL            COMMENT '题目评测时用户程序的额外文件 json key:name value:content',
    `judge_extra_file`mediumtext     DEFAULT NULL            COMMENT '题目评测时交互或特殊程序的额外文件 json key:name value:content',
    `spj_code`        longtext       DEFAULT NULL            COMMENT '特判程序或交互程序代码',
    `spj_language`    varchar(255)   DEFAULT NULL            COMMENT '特判程序或交互程序代码的语言',
    'del_flag'        boolean        DEFAULT 0               COMMENT  '删除标记(0未删除 1删除)',
    `create_time`     datetime       DEFAULT now()           COMMENT '创建时间',
    `update_time`     datetime       DEFAULT now()           COMMENT '更新时间，用于乐观锁',
    PRIMARY KEY (`id`),
    UNIQUE (problem_id)
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8 COMMENT '题目信息';

-- ----------------------------
-- 2、题目测试用例表
-- ----------------------------
drop table if exists problem_case;
CREATE TABLE `problem_case` (
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `pid`           bigint(20) unsigned NOT NULL COMMENT '题目id',
    `input`         longtext COMMENT '测试样例的输入',
    `output`        longtext COMMENT '测试样例的输出',
    `score`         int(11) DEFAULT NULL COMMENT '该测试样例的IO得分',
    `status`        int(11) DEFAULT '0' COMMENT '0可用，1不可用',
    `group_num`     int(11) DEFAULT '1' COMMENT 'subtask分组的编号',
    `create_time`   datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time`   datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8  COMMENT '测试用例';


-- ----------------------------
-- 3、题目标签表
-- ----------------------------
drop table if exists problem_tag;
CREATE TABLE `problem_tag` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `name` varchar(255) unique  comment '题目标签',
    `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;