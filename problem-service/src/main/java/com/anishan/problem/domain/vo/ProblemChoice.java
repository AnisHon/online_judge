package com.anishan.problem.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("选择题答案")
public class ProblemChoice {

    @ApiModelProperty("ABCD选项")
    private Character order;
    @ApiModelProperty("选项内容")
    private String content;

}
