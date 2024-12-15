package com.anishan.problem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 题目完成表
 * @TableName problem_complete
 */
@TableName(value ="problem_complete")
@Data
public class ProblemComplete implements Serializable {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 题目id
     */
    private Long problemId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}