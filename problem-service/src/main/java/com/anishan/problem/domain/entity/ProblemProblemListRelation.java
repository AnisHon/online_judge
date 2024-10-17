package com.anishan.problem.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 题单 题目关系表
 * @TableName problem_problem_list
 */
@TableName(value ="problem_problem_list")
@Data
public class ProblemProblemListRelation implements Serializable {
    /**
     * 单子id
     */
    private Long listId;

    /**
     * 题目id
     */
    private Long problemId;

    /**
     * 题目顺序
     */
    private Integer problemOrder;

    /**
     * 每道题对应分数
     */
    private BigDecimal score;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;


}