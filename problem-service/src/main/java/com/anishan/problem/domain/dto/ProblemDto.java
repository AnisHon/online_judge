package com.anishan.problem.domain.dto;

import com.anishan.commons.enumeration.ProblemAuth;
import com.anishan.commons.enumeration.ProblemType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * 题目主表，OJ题目有分表，非OJ不需要继续分表
 * @TableName problem
 */

@Data
@Accessors(chain = true)
public class ProblemDto {
    /**
     * 主键
     */
    @JsonSerialize(using = ToStringSerializer.class)
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
    private ProblemAuth auth;

}