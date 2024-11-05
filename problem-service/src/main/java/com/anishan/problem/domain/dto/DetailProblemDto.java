package com.anishan.problem.domain.dto;

import com.anishan.problem.domain.entity.OjProblemCase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("添加或更新问题")
public class DetailProblemDto {
    @ApiModelProperty("问题本体")
    private ProblemDto problem;
    @ApiModelProperty("OJ附加内容")
    private OjProblemDto ojProblem;
    @ApiModelProperty("选择题 填空题答案")
    private List<ChoiceFillAnswersDto> choices;
    @ApiModelProperty("OJ问题测试用例")
    private List<OjProblemCase> cases;
}
