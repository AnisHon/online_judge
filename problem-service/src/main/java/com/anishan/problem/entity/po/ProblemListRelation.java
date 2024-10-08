package com.anishan.problem.entity.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 题单 题目关系表
 * @TableName problem_list
 */
@TableName(value ="problem_list")
@Data
public class ProblemListRelation implements Serializable {
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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}