package com.anishan.user.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户表
 * @TableName sys_user
 */
@TableName(value ="sys_user")
@Data
public class SysUser implements Serializable {
    /**
     * 用户表主键
     */
    @TableId(type = IdType.AUTO)
    private Long userId;

    /**
     * 用户名，唯一
     */
    private String userName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 昵称,不唯一
     */
    private String nikeName;

    /**
     * 用户密码-加密
     */
    private String password;

    /**
     * 状态(1封禁, 0正常)
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最新更新时间用于乐观锁
     */
    private Date updateTime;

    /**
     * 删除标记(1删除, 0没删除)
     */
    private Integer delFlag;

    /**
     * 备注
     */
    private String remark;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}