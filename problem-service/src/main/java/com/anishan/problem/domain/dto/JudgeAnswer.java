package com.anishan.problem.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ApiModel("答案类")
@NoArgsConstructor
@AllArgsConstructor
public class JudgeAnswer {

    @ApiModelProperty("答案序号")
    private Integer index;
    @ApiModelProperty("答案")
    private String answer;


}
