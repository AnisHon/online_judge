package com.anishan.problem.entity.po;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 判题测试用例
 * @TableName problem_case
 */
@TableName(value ="problem_case")
@Data
public class ProblemCase implements Serializable {
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
     * 该测试样例的IO得分，如果支持OI模式的话
     */
    private Integer score;

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