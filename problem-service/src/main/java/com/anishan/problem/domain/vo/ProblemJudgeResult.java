package com.anishan.problem.domain.vo;

import com.anishan.problem.domain.JudgeAnswer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("答案结果")
public class ProblemJudgeResult {

    private boolean right;
    private List<JudgeAnswer> answers;

}
