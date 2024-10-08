package com.anishan.problem.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

import com.sun.mail.imap.protocol.BODY;
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
     * 当类型是其他的时候，这个就是题目答案
     */
    private Object answer;

    /**
     * 答对的分数
     */
    private Integer score;

    /**
     * 删除标记
     */

    private Integer delFlag;

    /**
     * 
     */
    private LocalDateTime createTime;

    /**
     * 
     */
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}