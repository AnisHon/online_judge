package com.anishan.problem.domain.entity;

import com.anishan.commons.enumeration.ProblemAuth;
import com.anishan.commons.enumeration.ProblemType;
import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 题目主表，OJ题目有分表，非OJ不需要继续分表
 * @TableName problem
 */
@TableName(value ="problem")
@Data
public class Problem implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long problemId;

    /**
     * OJ题目ID
     */
    private Long ojId;

    /**
     * 题目名称
     */
    private String title;

    /**
     * 题目类型，(1, 2, 3)
     */
    @EnumValue
    private ProblemType type;

    /**
     * 题目来源
     */
    private String source;

    /**
     * 题目描述
     */
    private String description;

    /**
     * 备注,提醒
     */
    private String hint;

    /**
     * 默认为1公开，2为比赛题目
     */
    @EnumValue
    private ProblemAuth auth;

    /**
     * 删除标记(0未删除 1删除)
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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;


}