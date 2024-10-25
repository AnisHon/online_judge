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

# www.github.com
insert into sys_user(sys_user.user_id, user_name, email, nike_name, password) values (1, 'test_teacher', 'teacher@tset.com', '测试教师', '$2a$10$cu.mwqY2JT1pGcIQM.h0R.GVi.yx8P4KC3UANgP7ypxsFaGxUR17m');
insert into sys_user(sys_user.user_id, user_name, email, nike_name, password) values (2, 'test_student', 'student@tset.com', '测试学生', '$2a$10$cu.mwqY2JT1pGcIQM.h0R.GVi.yx8P4KC3UANgP7ypxsFaGxUR17m');
insert into sys_user(sys_user.user_id, user_name, email, nike_name, password) values (3, 'test_admin', 'admin@tset.com', '测试管理员', '$2a$10$cu.mwqY2JT1pGcIQM.h0R.GVi.yx8P4KC3UANgP7ypxsFaGxUR17m');
insert into sys_user(sys_user.user_id, user_name, email, nike_name, password) values (4, 'test_super_admin', 'super_admin@tset.com', '超级管理员', '$2a$10$cu.mwqY2JT1pGcIQM.h0R.GVi.yx8P4KC3UANgP7ypxsFaGxUR17m');


-- ----------------------------
-- 2、角色信息表
-- ----------------------------
drop table if exists sys_role;
create table sys_role (
    role_id           bigint(20)      not null auto_increment    comment '角色ID',
    role_name         varchar(30)     not null unique            comment '角色名称',
    status            boolean         default 0                  comment '角色状态（0正常 1停用）',
    del_flag          boolean         default 0                  comment '删除标志（0代表存在 1代表删除）',
    create_time       datetime        default now()              comment '创建时间',
    update_time       datetime        default now()              comment '更新时间，用于乐观锁',
    remark            varchar(500)    default null               comment '备注',
    primary key (role_id)
) engine=innodb auto_increment=100 comment = '角色信息表';

insert into sys_role(role_id, role_name, remark) values (1, 'student', '学生');
insert into sys_role(role_id, role_name, remark) values (2, 'teacher', '教师');
insert into sys_role(role_id, role_name, remark) values (3, 'admin', '管理员');
insert into sys_role(role_id, role_name, remark) values (4, 'super_admin', '超级管理员');


-- ----------------------------
-- 3、班级信息表
-- ----------------------------
drop table if exists sys_class;
create table sys_class (
    class_id          bigint(20)      not null auto_increment    comment '角色ID',
    class_name        varchar(30)     not null unique            comment '角色名称',
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
    order_num         int(4)          not null                   comment '菜单顺序',
    parent_id         bigint(20)      default 0                  comment '父菜单ID',
    router            varchar(200)    default '#'                comment '路由路径',
    menu_type         char(1)         default ''                 comment '菜单类型（I菜单项item M菜单栏MenuBar B按钮）',
    perms             varchar(100)    default null               comment '权限Security标识',
    icon              varchar(100)    default '#'                comment '菜单图标',
    create_time       datetime        default now()              comment '创建时间',
    update_time       datetime        default now()              comment '更新时间',
    del_flag          boolean         default 0                  comment '删除标志（0未删除 1删除）',
    remark            varchar(500)    default ''                 comment '备注',
    primary key (menu_id)
) engine=innodb auto_increment=2000 comment = '菜单权限表';

# 一级菜单
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (1, '题目模块', 1, 0, '#', 'M', '#', 'Files');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (2, '用户模块', 2, 0, '#', 'M', '#', 'UserFilled');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (3, '教师功能', 0, 0, '#', 'M', '#', 'Notebook');

# 二集菜单
# menu_id 1 题目模块
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (10, '题目编辑', 1, 1, 'problem-edit', 'I', '#', 'Management');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (11, '标签编辑', 2, 1, 'tag-edit', 'I', '#', 'CollectionTag');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (12, '题单编辑', 3, 1, 'list-edit', 'I', '#', 'List');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (13, '目录编辑', 4, 1, 'folder-edit', 'I', '#', 'Folder');

# 二集菜单
# menu_id 2 用户模块
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (20, '用户管理', 1, 2, 'user-manage', 'I', '#', 'Avatar');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (21, '班级管理', 2, 2, 'class-manage', 'I', '#', 'DataBoard');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (22, '权限管理', 3, 2, 'auth-manage', 'I', '#', 'WarnTriangleFilled');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (23, '角色管理', 4, 2, 'role-manage', 'I', '#', 'WarningFilled');


# 二级菜单
# menu_id 2 教师功能 teacher
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (30, '我的班级', 1, 3, 'my-class', 'I', '#', 'School');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (31, '作业管理', 2, 3, 'homework-manage', 'I', '#', 'Histogram');


# menu_id 10 题目编辑
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (101, '添加标签', 1, 10, '#', 'B', 'problem:problem:add-tag', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (102, '删除标签', 2, 10, '#', 'B', 'problem:problem:del-tag', '#');
#  todo

# menu_id 11 标签编辑
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (110, '添加标签', 1, 11, '#', 'B', 'problem:tag:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (111, '删除标签', 2, 11, '#', 'B', 'problem:tag:delete', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (112, '更改标签', 3, 11, '#', 'B', 'problem:tag:update', '#');

# menu_id 12 题单编辑
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (120, '添加题单', 1, 12, '#', 'B', 'problem:list:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (121, '添加题目', 2, 12, '#', 'B', 'problem:list:add-problem', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (122, '删除题目', 3, 12, '#', 'B', 'problem:list:del-problem', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (123, '删除题单', 4, 12, '#', 'B', 'problem:list:delete', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (124, '修改题单', 5, 12, '#', 'B', 'problem:list:update', '#');

# menu_id 13 目录编辑
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (130, '添加目录', 1, 13, '#', 'B', 'problem:folder:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (131, '删除目录', 2, 13, '#', 'B', 'problem:folder:del', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (132, '修改目录', 3, 13, '#', 'B', 'problem:folder:update', '#');

# menu_id 20
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (200, '封禁用户', 1, 20, '#', 'B', 'user:auth:ban', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (201, '解封用户', 2, 20, '#', 'B', 'user:auth:unban', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (202, '添加用户', 3, 20, '#', 'B', 'user:user:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (203, '编辑用户', 4, 20, '#', 'B', 'user:user:edit', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (204, '列出用户', 5, 20, '#', 'B', 'user:user:list', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (205, '删除用户', 6, 20, '#', 'B', 'user:user:remove', '#');

# menu_id 21 班级管理
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (210, '删除用户', 1, 21, '#', 'B', 'user:class:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (211, '删除用户', 2, 21, '#', 'B', 'user:class:edit', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (212, '删除用户', 3, 21, '#', 'B', 'user:class:list', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (213, '删除用户', 3, 21, '#', 'B', 'user:class:remove', '#');

# menu_id 22 权限管理
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (220, '添加菜单', 1, 22, '#', 'B', 'user:menu:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (221, '编辑菜单', 2, 22, '#', 'B', 'user:menu:edit', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (222, '列出菜单', 3, 22, '#', 'B', 'user:menu:list', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (223, '删除菜单', 4, 22, '#', 'B', 'user:menu:remove', '#');

# menu_id 23 角色管理
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (230, '添加角色', 1, 23, '#', 'B', 'user:role:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (231, '编辑角色', 2, 23, '#', 'B', 'user:role:edit', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (232, '列出角色', 3, 23, '#', 'B', 'user:role:list', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (233, '删除角色', 4, 23, '#', 'B', 'user:role:remove', '#');

# 我的班级 menu_id 30
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (300, '创建班级', 0, 3, '#', 'B', 'user:teacher:create-class', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (301, '列出班级', 1, 3, '#', 'B', 'user:teacher:list-class', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (302, '删除班级', 2, 3, '#', 'B', 'user:teacher:remove-class', '#');



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
insert into sys_user_role values ('4', '4');

-- ----------------------------
-- 8、角色和菜单关联表  角色1-N菜单
-- ----------------------------
drop table if exists sys_role_menu;
create table sys_role_menu (
    role_id   bigint(20) not null comment '角色ID',
    menu_id   bigint(20) not null comment '菜单ID',
    primary key(role_id, menu_id)
) engine=innodb comment = '角色和菜单关联表';

# 超级管理员
insert into sys_role_menu(role_id, menu_id)
    (select 4, menu_id from sys_menu);

# 教师
insert into sys_role_menu(role_id, menu_id)
values
    (2, 30),
    (2, 31),
    (2, 32),
    (2, 3);

# 管理员，没有权限相关操作，因为危险可能会毁坏网站
insert into sys_role_menu(role_id, menu_id)
    (
        select 3, menu_id
        from sys_menu
        where
            menu_id not in (select menu_id from sys_role_menu where role_id = 2)
          and
            menu_id not in (220, 221, 222, 223, 230, 231, 232, 233)
    )








