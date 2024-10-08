package com.anishan.problem.entity.dto;

import com.anishan.commons.e.ValidationGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@ApiModel("判题测试用例")
public class ProblemCaseDto {

    @NotNull(groups = ValidationGroup.Update.class)
    @ApiModelProperty("主键id")
    private Long caseId;

    @ApiModelProperty("题目id")
    private Long problemId;

    @ApiModelProperty("测试样例的输入")
    private String input;

    @ApiModelProperty("测试样例的输出")
    private String output;

    @ApiModelProperty("当类型是其他的时候，这个就是题目答案")
    private Object answer;

    @ApiModelProperty("答对的分数")
    private Integer score;

}