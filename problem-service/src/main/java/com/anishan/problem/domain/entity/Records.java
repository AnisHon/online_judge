package com.anishan.problem.domain.entity;

import com.anishan.problem.domain.vo.UserAnswer;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 题目完成表
 * @TableName records
 */
@TableName(value ="records", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Records implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long recordId;

    /**
     * 比赛ID，非比赛可不填
     */
    private Long contestId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 题目id
     */
    private Long problemId;

    /**
     * 最终得分
     */
    private BigDecimal score;

    /**
     * 是否正确 0 1
     */
    private boolean status;

    /**
     * 答案
     */
    @TableField(typeHandler = JacksonTypeHandler.class, property = "answer", value = "answer")
    private UserAnswer answer;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}