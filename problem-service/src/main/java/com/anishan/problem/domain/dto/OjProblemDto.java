package com.anishan.problem.domain.dto;

import com.anishan.commons.e.Difficulty;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OJ题目分表
 * @TableName oj_problem
 */
@TableName(value ="oj_problem")
@Data
@Accessors(chain = true)
public class OjProblemDto {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long problemId;

    /**
     * 单位ms
     */
    private Integer timeLimit;

    /**
     * 难度 (0 未分类, 1 简单, 2 中等, 3 困难)
     */
    private Difficulty difficulty;

    /**
     * 单位kb
     */
    private Integer memoryLimit;

    /**
     * 单位mb
     */
    private Integer stackLimit;

    /**
     * 输入描述
     */
    private String input;

    /**
     * 输出描述
     */
    private String output;

    /**
     * 输入样例
     */
    private String inputExample;

    /**
     * 输出样例
     */
    private String outputExample;

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