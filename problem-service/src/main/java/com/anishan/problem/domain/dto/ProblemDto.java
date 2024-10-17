package com.anishan.problem.domain.dto;

import com.anishan.commons.e.ProblemAuth;
import com.anishan.commons.e.ProblemType;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题目主表，OJ题目有分表，非OJ不需要继续分表
 * @TableName problem
 */
@TableName(value ="problem")
@Data
public class ProblemDto {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long problemId;

    /**
     * 题目名称
     */
    private String title;

    /**
     * 题目类型，
     */
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

}