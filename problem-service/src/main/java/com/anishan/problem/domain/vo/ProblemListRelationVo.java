package com.anishan.problem.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 题单 题目关系表
 * @TableName problem_problem_list
 */

@Data
@ApiModel("题单里面对应的每条问题")
@AllArgsConstructor
@NoArgsConstructor
public class ProblemListRelationVo {

    @ApiModelProperty("题目")
    private ProblemVo problem;

    @ApiModelProperty("题目顺序")
    private Integer problemOrder;

    @ApiModelProperty("对应分数")
    private BigDecimal score;

}