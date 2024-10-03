drop database if exists db_user;
create database db_user character set utf8mb4;
use db_user;



-- ----------------------------
-- 1、用户信息表
-- ----------------------------
drop table if exists sys_user;
create table sys_user (
    user_id     bigint(20)             not null auto_increment  comment '用户表主键',
    user_name   varchar(32)            not null                 comment '用户名，唯一',
    email       varchar(255)           not null                 comment '邮箱',
    nike_name   varchar(32)            not null                 comment '昵称,不唯一',
    password    varchar(255)           not null                 comment '用户密码-加密',
    status      boolean  default 0     not null                 comment '状态(1封禁, 0正常)',
    create_time datetime default now() not null                 comment '创建时间',
    update_time datetime default now() not null                 comment '最新更新时间用于乐观锁',
    del_flag    boolean  default 0     not null                 comment '删除标记(1删除, 0没删除)',
    remark      varchar(500) default null                       comment '备注',
    primary key sys_user(user_id),
    constraint sys_user_pk_2
        unique (user_name),
    constraint sys_user_pk_3
        unique (email)
) engine=innodb auto_increment=100
    comment '用户表' auto_increment = 100;

insert into sys_user(sys_user.user_id, user_name, email, nike_name, password) values (1, 'test_teacher', 'teacher@tset.com', '测试教师', '');
insert into sys_user(sys_user.user_id, user_name, email, nike_name, password) values (2, 'test_student', 'student@tset.com', '测试学生', '');
insert into sys_user(sys_user.user_id, user_name, email, nike_name, password) values (3, 'test_admin', 'admin@tset.com', '测试管理员', '');


-- ----------------------------
-- 2、角色信息表
-- ----------------------------
drop table if exists sys_role;
create table sys_role (
    role_id           bigint(20)      not null auto_increment    comment '角色ID',
    role_name         varchar(30)     not null unique            comment '角色名称',
    status            boolean         default 0                  comment '角色状态（0正常 1停用）',
    del_flag          boolean         default 0                  comment '删除标志（0代表存在 2代表删除）',
    create_time       datetime        default now()              comment '创建时间',
    update_time       datetime        default now()              comment '更新时间，用于乐观锁',
    remark            varchar(500)    default null               comment '备注',
    primary key (role_id)
) engine=innodb auto_increment=100 comment = '角色信息表';

insert into sys_role(role_id, role_name, remark) values (1, 'student', '学生');
insert into sys_role(role_id, role_name, remark) values (2, 'teacher', '教师');
insert into sys_role(role_id, role_name, remark) values (3, 'admin', '管理员');


-- ----------------------------
-- 3、班级信息表
-- ----------------------------
drop table if exists sys_class;
create table sys_class (
    class_id          bigint(20)      not null auto_increment    comment '角色ID',
    class_name        varchar(30)     not null                   comment '角色名称',
    del_flag          boolean         default 0                  comment '删除标志(0代表存在 2代表删除)',
    create_time       datetime        default now()              comment '创建时间',
    update_time       datetime        default now()              comment '更新时间，用于乐观锁',
    remark            varchar(500)    default null               comment '备注',
    primary key (class_id)
) engine=innodb auto_increment=100 comment = '班级信息表';


-- ---------------------------
-- 4、教师(用户)班级关系表
-- ----------------------------
drop table if exists teacher_class;
create table teacher_class (
    teacher_id   bigint(20) not null comment '教师ID(user_id)',
    class_id     bigint(20) not null comment '班级ID',
    primary key(teacher_id, class_id)
) engine=innodb comment = '教师班级关系表';


-- ---------------------------
-- 5、学生(用户)班级关系表
-- ----------------------------
drop table if exists student_class;
create table student_class (
   student_id   bigint(20) not null comment '学生ID(user_id)',
   class_id     bigint(20) not null comment '班级ID',
   primary key(student_id, class_id)
) engine=innodb comment = '学生班级关系表';

-- ----------------------------
-- 6、菜单权限表
-- ----------------------------
drop table if exists sys_menu;
create table sys_menu (
    menu_id           bigint(20)      not null auto_increment    comment '菜单ID',
    menu_name         varchar(50)     not null                   comment '菜单名称',
    parent_id         bigint(20)      default 0                  comment '父菜单ID',
    router            varchar(200)    default '#'                comment '路由路径',
    menu_type         char(1)         default ''                 comment '菜单类型（I菜单项item M菜单栏MenuBar B按钮）',
    perms             varchar(100)    default null               comment '权限Security标识',
    icon              varchar(100)    default '#'                comment '菜单图标',
    create_time       datetime        default now()              comment '创建时间',
    update_time       datetime        default now()              comment '更新时间',
    remark            varchar(500)    default ''                 comment '备注',
    primary key (menu_id)
) engine=innodb auto_increment=2000 comment = '菜单权限表';

-- ----------------------------
-- 7、用户和角色关联表  用户N-1角色
-- ----------------------------
drop table if exists sys_user_role;
create table sys_user_role (
    user_id   bigint(20) not null comment '用户ID',
    role_id   bigint(20) not null comment '角色ID',
    primary key(user_id, role_id)
) engine=innodb comment = '用户和角色关联表';

-- ----------------------------
-- 初始化-用户和角色关联表数据
-- ----------------------------
insert into sys_user_role values ('1', '1');
insert into sys_user_role values ('2', '2');
insert into sys_user_role values ('3', '3');


-- ----------------------------
-- 8、角色和菜单关联表  角色1-N菜单
-- ----------------------------
drop table if exists sys_role_menu;
create table sys_role_menu (
    role_id   bigint(20) not null comment '角色ID',
    menu_id   bigint(20) not null comment '菜单ID',
    primary key(role_id, menu_id)
) engine=innodb comment = '角色和菜单关联表';








