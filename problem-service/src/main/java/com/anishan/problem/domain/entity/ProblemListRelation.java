package com.anishan.problem.domain.entity;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.problem.domain.vo.ProblemListRelationVo;
import com.anishan.problem.domain.vo.ProblemVo;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 题单 题目关系表
 * @TableName problem_problem_list
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProblemListRelation implements Serializable {

    private Problem problem;

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

    public ProblemListRelationVo toVo() {

        ProblemVo problemVo = BeanUtil.copyProperties(this.problem, ProblemVo.class);
        return new ProblemListRelationVo(problemVo, this.problemOrder, this.score);
    }



}