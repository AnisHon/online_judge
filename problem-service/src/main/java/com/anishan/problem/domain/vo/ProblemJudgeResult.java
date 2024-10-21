package com.anishan.problem.domain.vo;

import com.anishan.problem.domain.JudgeAnswer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@ApiModel("答案结果")
public class ProblemJudgeResult {

    @ApiModelProperty("是否正确")
    private boolean correct;

    @ApiModelProperty("标准答案")
    private List<JudgeAnswer> answers;

    @ApiModelProperty("总分")
    private BigDecimal totalScore;

}
