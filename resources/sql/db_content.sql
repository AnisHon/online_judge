-- ----------------------------
-- 资源服务的数据库
-- ----------------------------
# drop database if exists db_content;
create database if not exists db_content character set utf8mb4;
use db_content;

-- ----------------------------
-- 1.文件信息表
-- ----------------------------
drop table if exists file_info;
create table file_info (
    file_id     bigint(20)      not null primary key    comment '主键',
    file_name   varchar(255)    not null                comment '文件名',
    file_path   varchar(512)    not null unique         comment '文件路径',
    file_size   bigint          not null default 0      comment '文件大小默认0，单位byte',
    file_md5    char(32)        not null                comment '文件MD5值',
    reference   bigint          not null default 0      comment '引用计数',
    file_type   varchar(32)     not null                comment '文件类型',
    upload_time datetime        not null default current_timestamp
) ENGINE=InnoDB default charset=utf8 comment '文件信息表';
create index file_md5_idx on file_info(file_md5);
create index file_name_idx on file_info(file_name);


-- ----------------------------
-- 2.公告表
-- ----------------------------
drop table if exists notice;
create table notice (
    notice_id   bigint(20)      not null primary key    comment '主键',
    title       varchar(255)    not null                comment '公告标题',
    user_id     bigint(20)      not null                comment '发送者ID',
    top_up      boolean         not null                comment '是否置顶',
    del_flag    boolean         not null default 0      comment '逻辑删除',
    create_time datetime        not null default current_timestamp,
    update_time datetime        not null default current_timestamp
) ENGINE=InnoDB default charset=utf8 comment '文件信息表';

-- ----------------------------
-- 3.公告内容表
-- ----------------------------
drop table if exists notice_content;
create table notice_content (
    notice_id   bigint  not null primary key    comment '公告ID',
    content     text    not null                comment '公告内容',
    constraint notice_content_id_fk foreign key notice_content(notice_id)
        references notice(notice_id)
) ENGINE=InnoDB default charset=utf8 comment '公告内容表';

-- ----------------------------
-- 4.文件目录链接表
-- ----------------------------
drop table if exists file_link;
create table file_link (
    link_id     bigint(20)      not null primary key    comment '主键',
    file_id     bigint(20)      not null                comment '文件id',
    reference   bigint(20)      not null default 0      comment '引用计数',
    constraint file_link_file_id_fk foreign key file_link(file_id)
        references file_info(file_id)
) ENGINE=InnoDB default charset=utf8 comment '文件目录链接表';

-- ----------------------------
-- 5.轮播图表
-- ----------------------------
drop table if exists carousel;
create table carousel (
    carousel_id bigint(20)      not null primary key    comment '主键',
    title       varchar(255)    not null                comment '标题',
    description varchar(255)    not null                comment '描述',
    image       varchar(512)    not null                comment '图片链接',
    link        varchar(512)    not null                comment '跳转链接',
    del_flag    boolean default 0 not null              comment '删除标记',
    create_time datetime        not null default current_timestamp,
    update_time datetime        not null default current_timestamp
) ENGINE=InnoDB default charset=utf8 comment '轮播图表';


-- ----------------------------
-- 6.商品表
-- ----------------------------
drop table if exists item;
create table item (
    item_id     bigint(20)      not null primary key    comment '主键',
    title       varchar(255)    not null                comment '标题',
    image       varchar(512)    not null                comment '图片链接',
    link        varchar(512)    not null                comment '跳转链接',
    del_flag    boolean default 0 not null              comment '删除标记',
    create_time datetime        not null default current_timestamp,
    update_time datetime        not null default current_timestamp
) ENGINE=InnoDB default charset=utf8 comment '轮播图表';


-- ----------------------------
-- 7.网盘文件表 parent = 0 的时候表示没有父目录
-- ----------------------------
drop table if exists cloud_files;
create table cloud_files (
    cloud_file_id   bigint(20)      not null primary key    comment '主键',
    file_name       varchar(255)    not null                comment '文件名',
    parent_id       bigint(20)      not null                comment '父文件ID',
    dir             boolean         not null default false  comment '是否是文件夹',
    file_id         bigint(20)      null                    comment '存储文件ID',
    user_id         bigint(20)      not null                comment '用户ID',
    create_time     datetime        not null default current_timestamp,
    update_time     datetime        not null default current_timestamp,
    unique (parent_id, file_name),
    unique (parent_id, cloud_file_id),
    constraint cloud_files_file_id foreign key cloud_files(file_id)
        references file_info(file_id) on delete cascade
) ENGINE=InnoDB default charset=utf8 comment '网盘文件表';

-- ----------------------------
-- 8.分片上传记录
-- ----------------------------
drop table if exists chunk_upload;
CREATE TABLE chunk_upload (
    chunk_id    bigint          not null,
    upload_id   varchar(255)    not null comment '分片上传的uploadId',
    chunk_size  bigint          not null comment '每个分片大小（byte）',
    chunk_num   int             not null comment '分片数量',
    primary key (chunk_id),
    unique key (upload_id),
    constraint chunk_upload_file_id foreign key chunk_upload(chunk_id)
        references file_info(file_id) on delete cascade
) engine=InnoDB default charset=utf8mb4 comment='分片上传记录';
