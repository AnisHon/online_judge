package com.anishan.problem.domain.entity;

import com.anishan.problem.domain.dto.ProblemTagDto;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签 题目关系表
 * @TableName problem_tag
 */
@TableName(value ="problem_tag")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProblemTagRelation implements Serializable {

    private ProblemTagRelation(ProblemTagDto problemTagDto) {
        this.problemId = problemTagDto.getProblemId();
        this.tagId = problemTagDto.getTagId();
    }
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