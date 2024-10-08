package com.anishan.problem.entity.po;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 题目类型信息表
 * @TableName sys_problem_type
 */
@TableName(value ="sys_problem_type")
@Data
public class SysProblemType implements Serializable {
    /**
     * 类型id
     */
    @TableId(type = IdType.AUTO)
    private Long typeId;

    /**
     * 类型名
     */
    private String typeName;

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
     * 逻辑删除
     */
    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}