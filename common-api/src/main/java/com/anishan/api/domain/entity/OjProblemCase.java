package com.anishan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * OJ判题测试用例
 * @TableName oj_problem_case
 */
@TableName(value ="oj_problem_case")
@Data
@Accessors(chain = true)
public class OjProblemCase implements Serializable {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Long caseId;

    /**
     * 题目id
     */
    private Long problemId;

    /**
     * 测试样例的输入
     */
    private String input;

    /**
     * 测试样例的输出
     */
    private String output;

    /**
     * 答对的分数
     */
    private BigDecimal score;

    /**
     * 删除标记
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 
     */
    private LocalDateTime createTime;

    /**
     *
     */
    @Version
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}