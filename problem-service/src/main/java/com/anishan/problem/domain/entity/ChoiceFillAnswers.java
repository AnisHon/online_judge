package com.anishan.problem.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 填空选择题答案表
 * @TableName choice_fill_answers
 */
@TableName(value ="choice_fill_answers")
@Data
public class ChoiceFillAnswers implements Serializable {
    /**
     * 主键id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long answerId;

    /**
     * 题目id
     */
    private Long problemId;

    /**
     * 选项或填空答案
     */
    private String answerText;

    /**
     * 填空题空格索引, 选择题ABCD索引 1表示A
     */
    private Boolean isCorrect;

    /**
     * 对于填空题，标识是第几个空格（填空题专用）
     */
    private Integer blankIndex;

    /**
     * 题目某个空的分数
     */
    private BigDecimal score;

    /**
     * 删除标记
     */
    @TableLogic
    private Integer delFlag;

    private LocalDateTime createTime;

    @Version
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public Character indexToChoice() {
        return (char) ('A' + blankIndex - 1);
    }

}