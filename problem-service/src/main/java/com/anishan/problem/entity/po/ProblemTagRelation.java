package com.anishan.problem.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 标签 题目关系表
 * @TableName problem_tag
 */
@TableName(value ="problem_tag")
@Data
public class ProblemTagRelation implements Serializable {
    /**
     * 题目id
     */
    private Long problemId;

    /**
     * 标签id
     */
    private Long tagId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}