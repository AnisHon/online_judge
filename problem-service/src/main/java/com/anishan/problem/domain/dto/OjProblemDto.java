package com.anishan.problem.domain.dto;

import com.anishan.commons.enumeration.Difficulty;
import com.anishan.commons.enumeration.ValidationGroup;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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
    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;

    /**
     * 单位ms
     */
    @Min(value = 1, groups = ValidationGroup.Insert.class)
    private Integer timeLimit;

    /**
     * 难度 (0 未分类, 1 简单, 2 中等, 3 困难)
     */
    @NotNull(groups = ValidationGroup.Insert.class)
    private Difficulty difficulty;

    /**
     * 单位kb
     */
    @Min(value = 1, groups = ValidationGroup.Insert.class)
    private Integer memoryLimit;

    /**
     * 单位mb
     */
    @Min(value = 1, groups = ValidationGroup.Insert.class)
    private Integer stackLimit;

    /**
     * 输入描述
     */
    @NotEmpty(groups = ValidationGroup.Insert.class)
    private String input;

    /**
     * 输出描述
     */
    @NotEmpty(groups = ValidationGroup.Insert.class)
    private String output;

    /**
     * 输入样例
     */
    private String inputExample;

    /**
     * 输出样例
     */
    @NotEmpty(groups = ValidationGroup.Insert.class)
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