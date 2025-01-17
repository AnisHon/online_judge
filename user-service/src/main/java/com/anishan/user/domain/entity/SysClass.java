package com.anishan.user.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 班级信息表
 * @TableName sys_class
 */
@TableName(value ="sys_class")
@Data
public class SysClass implements Serializable {
    /**
     * 角色ID
     */
    @TableId(type = IdType.AUTO)
    private Long classId;

    /**
     * 角色名称
     */
    private String className;

    /**
     * 删除标志(0 代表存在 2 代表删除)
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间，用于乐观锁
     */
    @Version
    private LocalDateTime updateTime;

    /**
     * 备注
     */
    private String remark;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}