package com.anishan.problem.domain.vo;

import com.anishan.commons.e.JudgeResult;
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

    @ApiModelProperty("OJ测评结果 (AC Accept RE RuntimeError WA WrongAnswer TLE TimeLimitExceed MLE MemoryLimitExceed CE CompileError)")
    private JudgeResult judgeResult;

    @ApiModelProperty("错误信息，std err的信息")
    private String errorMessage;

    @ApiModelProperty("标准答案")
    private List<JudgeAnswer> answers;

    @ApiModelProperty("总分")
    private BigDecimal totalScore;

    @ApiModelProperty("满分")
    private BigDecimal fullMark;

    public void add(BigDecimal num) {
        this.totalScore = this.totalScore.add(num);
    }

}
