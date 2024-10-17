package com.anishan.problem.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@ApiModel("题目表详情表用于题目页面")
@AllArgsConstructor
@NoArgsConstructor
public class DetailProblem {
    @ApiModelProperty("问题ID")
    private ProblemVo problemVo;
    @ApiModelProperty("OJ问题ID")
    private OjProblemVo ojProblemVo;
    @ApiModelProperty("选择问题的选项")
    private List<ProblemChoice> choices;
}
