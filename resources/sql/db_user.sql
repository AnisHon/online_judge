-- ----------------------------
-- 用户服务的数据库
-- ----------------------------
# drop database if exists db_user;
create database db_user character set utf8mb4;
use db_user;



-- ----------------------------
-- 1、用户信息表
-- ----------------------------
drop table if exists sys_user;
create table sys_user (
    user_id     bigint(20)             not null auto_increment  comment '用户表主键',
    user_name   varchar(32)            not null                 comment '用户名，唯一',
    email       varchar(255)           null                     comment '邮箱',
    nike_name   varchar(32)            not null                 comment '昵称,不唯一',
    password    varchar(255)           not null                 comment '用户密码-加密',
    status      boolean  default 0     not null                 comment '状态(1封禁, 0正常)',
    points      Decimal(8, 2) default 0 not null                comment '用户积分',
    create_time datetime default now() not null                 comment '创建时间',
    update_time datetime default now() not null                 comment '最新更新时间用于乐观锁',
    del_flag    boolean  default 0     not null                 comment '删除标记(1删除, 0没删除)',
    remark      varchar(500) default null                       comment '备注',
    primary key sys_user(user_id),
    constraint sys_user_pk_2
        unique (user_name)
) engine=innodb auto_increment=100
    comment '用户表' auto_increment = 100;
create unique index unique_user_email on sys_user(email);
# 默认密码：www.github.com
insert into sys_user(sys_user.user_id, user_name, email, nike_name, password) values (1, 'teacher', null, '测试教师', '$2a$10$cu.mwqY2JT1pGcIQM.h0R.GVi.yx8P4KC3UANgP7ypxsFaGxUR17m');
insert into sys_user(sys_user.user_id, user_name, email, nike_name, password) values (2, 'student', null, '测试学生', '$2a$10$cu.mwqY2JT1pGcIQM.h0R.GVi.yx8P4KC3UANgP7ypxsFaGxUR17m');
insert into sys_user(sys_user.user_id, user_name, email, nike_name, password) values (3, 'admin', null, '测试管理员', '$2a$10$cu.mwqY2JT1pGcIQM.h0R.GVi.yx8P4KC3UANgP7ypxsFaGxUR17m');
insert into sys_user(sys_user.user_id, user_name, email, nike_name, password) values (4, 'super_admin', null, '超级管理员', '$2a$10$cu.mwqY2JT1pGcIQM.h0R.GVi.yx8P4KC3UANgP7ypxsFaGxUR17m');


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



-- ---------------------------
-- 5、用户班级关系表
-- ----------------------------
drop table if exists student_class;
create table student_class (
    student_id   bigint(20) not null comment '学生ID(user_id)',
    class_id     bigint(20) not null comment '班级ID',
    primary key(student_id, class_id),
    constraint class_student_id_pk foreign key student_class(student_id)
        references sys_user(user_id),
    constraint class_class_id_pk foreign key sys_class(class_id)
        references sys_class(class_id)
) engine=innodb comment = '学生班级关系表';

-- ----------------------------
-- 6、菜单权限表
-- ----------------------------
drop table if exists sys_menu;
create table sys_menu (
    menu_id           bigint(20)      not null auto_increment    comment '菜单ID',
    menu_name         varchar(50)     not null                   comment '菜单名称',
    order_num         int(4)          not null                   comment '菜单顺序',
    parent_id         bigint(20)      default null               comment '父菜单ID',
    router            varchar(200)    default '#'                comment '路由路径',
    menu_type         char(1)         default 'M'                comment '菜单类型（I菜单项item M菜单栏MenuBar B按钮）',
    perms             varchar(100)    default '#'                comment '权限Security标识',
    icon              varchar(100)    default '#'                comment '菜单图标',
    component         varchar(255)    default null               comment '组件路径',
    create_time       datetime        default now()              comment '创建时间',
    update_time       datetime        default now()              comment '更新时间',
    del_flag          boolean         default 0                  comment '删除标志（0未删除 1删除）',
    remark            varchar(500)    default ''                 comment '备注',
    primary key (menu_id),
    constraint menu_menu_id_pk foreign key sys_menu(parent_id)
                      references sys_menu(menu_id)
) engine=innodb auto_increment=2000 comment = '菜单权限表';

# 一级菜单
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (1, '题目模块', 1, null, 'problem-module', 'M', '#', 'Files', null);
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (2, '用户模块', 2, null, 'user-module', 'M', '#', 'UserFilled', null);
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (3, '教师功能', 0, null, 'teacher', 'M', '#', 'Notebook', null);
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (4, '系统管理', 0, null, 'system-manage', 'M', '#', 'Setting', null);

# 二集菜单
# menu_id 1 题目模块
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (10, '题目编辑', 1, 1, 'problem-edit', 'I', '#', 'Management', 'backend/problem-module/problem-edit/ProblemEdit');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (11, '标签编辑', 2, 1, 'tag-edit', 'I', '#', 'CollectionTag', 'backend/problem-module/tag-edit/TagEdit');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (12, '题单编辑', 3, 1, 'list-edit', 'I', '#', 'List', 'backend/problem-module/list-edit/ListEdit');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (13, '目录编辑', 4, 1, 'folder-edit', 'I', '#', 'Folder', 'backend/problem-module/folder-edit/FolderEdit');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (14, '题解编辑', 5, 1, 'solution-edit', 'I', '#', 'EditPen', 'backend/problem-module/solution-edit/SolutionEdit');

# 二集菜单
# menu_id 2 用户模块
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (20, '用户管理', 1, 2, 'user-manage', 'I', '#', 'Avatar', 'backend/user-module/user-manage/UserManage');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (21, '班级管理', 2, 2, 'class-manage', 'I', '#', 'DataBoard', 'backend/user-module/class-manage/ClassManage');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (22, '权限管理', 3, 2, 'auth-manage', 'I', '#', 'WarnTriangleFilled', 'backend/user-module/auth-manage/AuthManage');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (23, '角色管理', 4, 2, 'role-manage', 'I', '#', 'WarningFilled', 'backend/user-module/role-manage/RoleManage');


# 二级菜单
# menu_id 3 教师功能 teacher
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (30, '用户组', 1, 3, 'my-class', 'I', '#', 'School', 'backend/teacher/my-class/MyClass');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (31, '作业管理', 2, 3, 'homework-manage', 'I', '#', 'Histogram', 'backend/teacher/homework-manage/HomeworkManage');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (32, '竞赛管理', 3, 3, 'contest-manage', 'I', '#', 'Flag', 'backend/teacher/contest-manage/ContestManage');

# 二级菜单
# menu_id 4 系统管理 system-manage
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (41, '缓存管理', 1, 4, 'cache-manage', 'I', '#', 'RefreshRight', 'backend/system/cache-manage/CacheManage');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (42, '文件管理', 2, 4, 'file-manage', 'I', '#', 'Files', 'backend/system/file-manage/FileManage');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (43, '通知管理', 3, 4, 'notice-manage', 'I', '#', 'Notification', 'backend/system/notice-manage/NoticeManage');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (44, '判题记录', 4, 4, 'judge-submit-log', 'I', 'problem:judge:submit:read', 'DocumentChecked', 'backend/system/judge-submit-log/JudgeSubmitLog');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon, component) values (45, '判题日志', 5, 4, 'judge-case-log', 'I', 'problem:judge:case:read', 'DataAnalysis', 'backend/system/judge-case-log/JudgeCaseLog');


# menu_id 10 题目编辑
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (101, '添加标签', 1, 10, '#', 'B', 'problem:problem:add-tag', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (102, '删除标签', 2, 10, '#', 'B', 'problem:problem:del-tag', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (103, '列出题目', 3, 10, '#', 'B', 'problem:problem:list', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (104, '添加题目', 4, 10, '#', 'B', 'problem:problem:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (105, '删除题目', 5, 10, '#', 'B', 'problem:problem:remove', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (106, '更改题目', 6, 10, '#', 'B', 'problem:problem:edit', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (150, '查看本人提交', 7, 10, '#', 'B', 'problem:submit:read', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (151, '查看判题内部日志', 8, 10, '#', 'B', 'problem:judge:case:read', '#');

# menu_id 11 标签编辑
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (110, '添加标签', 1, 11, '#', 'B', 'problem:tag:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (111, '删除标签', 2, 11, '#', 'B', 'problem:tag:remove', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (112, '更改标签', 3, 11, '#', 'B', 'problem:tag:edit', '#');

# menu_id 12 题单编辑
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (120, '添加题单', 1, 12, '#', 'B', 'problem:list:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (121, '添加题目', 2, 12, '#', 'B', 'problem:list:add-problem', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (122, '删除题目', 3, 12, '#', 'B', 'problem:list:del-problem', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (123, '删除题单', 4, 12, '#', 'B', 'problem:list:remove', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (124, '修改题单', 5, 12, '#', 'B', 'problem:list:edit', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (125, '列出题单', 5, 12, '#', 'B', 'problem:list:list', '#');

# menu_id 13 目录编辑
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (130, '添加目录', 1, 13, '#', 'B', 'problem:folder:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (131, '删除目录', 2, 13, '#', 'B', 'problem:folder:remove', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (132, '修改目录', 3, 13, '#', 'B', 'problem:folder:edit', '#');

# menu_id 14 题解编辑
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (140, '修改题解', 1, 14, '#', 'B', 'problem:solution:edit', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (141, '查看题解', 2, 14, '#', 'B', 'problem:solution:list', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (142, '修改题解', 3, 14, '#', 'B', 'problem:solution:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (143, '删除题解', 4, 14, '#', 'B', 'problem:solution:remove', '#');

# menu_id 20
# 用户管理
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (200, '封禁用户', 1, 20, '#', 'B', 'user:auth:ban', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (201, '解封用户', 2, 20, '#', 'B', 'user:auth:unban', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (202, '添加用户', 3, 20, '#', 'B', 'user:user:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (203, '编辑用户', 4, 20, '#', 'B', 'user:user:edit', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (204, '列出用户', 5, 20, '#', 'B', 'user:user:list', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (205, '删除用户', 6, 20, '#', 'B', 'user:user:remove', '#');

# menu_id 21 班级管理
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (210, '添加班级', 1, 21, '#', 'B', 'user:class:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (211, '编辑班级', 2, 21, '#', 'B', 'user:class:edit', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (212, '列出班级', 3, 21, '#', 'B', 'user:class:list', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (213, '删除班级', 3, 21, '#', 'B', 'user:class:remove', '#');

# menu_id 22 权限管理
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (220, '添加菜单', 1, 22, '#', 'B', 'user:menu:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (221, '编辑菜单', 2, 22, '#', 'B', 'user:menu:edit', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (222, '列出菜单', 3, 22, '#', 'B', 'user:menu:list', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (223, '删除菜单', 4, 22, '#', 'B', 'user:menu:remove', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (224, '授予权限', 5, 22, '#', 'B', 'user:menu:grant', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (225, '撤销权限', 6, 22, '#', 'B', 'user:menu:revoke', '#');

# menu_id 23 角色管理
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (230, '添加角色', 1, 23, '#', 'B', 'user:role:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (231, '编辑角色', 2, 23, '#', 'B', 'user:role:edit', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (232, '列出角色', 3, 23, '#', 'B', 'user:role:list', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (233, '删除角色', 4, 23, '#', 'B', 'user:role:remove', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (234, '授予角色', 5, 23, '#', 'B', 'user:role:grant', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (235, '撤销角色', 6, 23, '#', 'B', 'user:role:revoke', '#');

# 我的班级 menu_id 30
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (300, '创建班级', 0, 30, '#', 'B', 'user:teacher:create-class', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (301, '列出班级', 1, 30, '#', 'B', 'user:teacher:list-class', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (302, '删除班级', 2, 30, '#', 'B', 'user:teacher:remove-class', '#');


# 比赛管理 menu_id 32
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (320, '查询比赛', 0, 32, '#', 'B', 'problem:contest:list', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (321, '添加比赛', 1, 32, '#', 'B', 'problem:contest:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (322, '编辑比赛', 2, 32, '#', 'B', 'problem:contest:edit', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (323, '删除比赛', 3, 32, '#', 'B', 'problem:contest:remove', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (324, '统计', 4, 32, '#', 'B', 'problem:contest:statistic', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (325, '排名', 5, 32, '#', 'B', 'problem:contest:rank', '#');

# 缓存管理 menu_id 41
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (410, '查看缓存', 1, 41, '#', 'B', 'content:cache:list', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (412, '删除缓存', 2, 41, '#', 'B', 'content:cache:remove', '#');


# 文件管理 menu_id 42
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (420, '列出文件', 1, 42, '#', 'B', 'content:file:list', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (421, '删除文件', 2, 42, '#', 'B', 'content:file:remove', '#');

# 通知管理 menu_id 43
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (431, '添加公告', 1, 43, '#', 'B', 'content:notice:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (432, '编辑公告', 2, 43, '#', 'B', 'content:notice:edit', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (433, '删除公告', 3, 43, '#', 'B', 'content:notice:remove', '#');


# 文件上传下载删除
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (1001, '添加文件', 0, null, '#', 'B', 'content:file:add', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (1002, '删除文件', 1, null, '#', 'B', 'content:file:remove', '#');
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (1003, '更改文件名', 2, null, '#', 'B', 'content:file:edit', '#');

# 信息展示
insert into sys_menu(menu_id, menu_name, order_num, parent_id, router, menu_type, perms, icon) values (1100, '信息查看', 1, null, '#', 'B', 'content:info', '#');


-- ----------------------------
-- 7、用户和角色关联表  用户 N-1 角色
-- ----------------------------
drop table if exists sys_user_role;
create table sys_user_role (
    user_id   bigint(20) not null comment '用户ID',
    role_id   bigint(20) not null comment '角色ID',
    primary key(user_id, role_id),
    constraint user_role_user_id_pk foreign key sys_user_role(user_id)
                           references sys_user(user_id),
    constraint user_role_role_id_pk foreign key sys_user_role(role_id)
                           references sys_role(role_id)
) engine=innodb comment = '用户和角色关联表';

-- ----------------------------
-- 初始化-用户和角色关联表数据
-- ----------------------------
insert into sys_user_role values ('2', '1');
insert into sys_user_role values ('1', '2');
insert into sys_user_role values ('3', '3');
insert into sys_user_role values ('4', '4');

-- ----------------------------
-- 8、角色和菜单关联表  角色1-N菜单
-- ----------------------------
drop table if exists sys_role_menu;
create table sys_role_menu (
    role_id   bigint(20) not null comment '角色ID',
    menu_id   bigint(20) not null comment '菜单ID',
    primary key(role_id, menu_id),
    constraint role_menu_menu_id_pk foreign key sys_role_menu(role_id)
        references sys_role(role_id),
    constraint role_menu_role_id_pk foreign key sys_role_menu(menu_id)
        references sys_menu(menu_id)

) engine=innodb comment = '角色和菜单关联表';

# 超级管理员
delete from sys_role_menu where role_id = 4;
insert into sys_role_menu(role_id, menu_id)
    (select 4, menu_id from sys_menu);

# 教师
delete from sys_role_menu where role_id = 2;
insert into sys_role_menu(role_id, menu_id)
values
    (2, 30),
    (2, 31),
    (2, 32),
    (2, 320),
    (2, 321),
    (2, 322),
    (2, 323),
    (2, 324),
    (2, 325),
    (2, 1001),
    (2, 1002),
    (2, 1003),
    (2, 1100);

# 所有登录用户可查看自己的提交；内部测试用例日志只授予管理员。
insert ignore into sys_role_menu(role_id, menu_id) values (1, 150), (2, 150), (3, 150), (4, 150), (3, 151), (4, 151);

# 管理员，没有权限相关操作，权限操作危险，可能会毁坏网站
delete from sys_role_menu where role_id = 3;
insert into sys_role_menu(role_id, menu_id)
    (
        select 3, menu_id
        from sys_menu
        where
            menu_id not in (select menu_id from sys_role_menu where role_id = 2)
    );



-- ----------------------------
-- 9、角色和菜单关联表  角色1-N菜单
-- ----------------------------
drop table if exists user_check_in;
create table user_check_in (
    id              bigint auto_increment comment '主键ID' primary key,
    user_id         bigint                 not null comment '用户ID',
    reward_point    decimal(8, 2)          not null comment '奖励积分个数',
    sign_time       date                   not null,
    current_time_   datetime default now() not null comment '签到当时时间',
    continuity_days int                    not null comment '连续签到天数',
    unique (user_id, sign_time),
    constraint check_in_user_id_pk foreign key user_check_in(user_id)
                           references sys_user(user_id)
) engine=innodb comment = '签到记录表';
