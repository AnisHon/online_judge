package com.anishan.problem.domain.dto;

import com.anishan.api.domain.entity.OjProblemCase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.simpleframework.xml.core.Validate;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@ApiModel("添加或更新问题")
@Accessors(chain = true)
public class DetailProblemDto {
    @Validate
    @ApiModelProperty("问题本体")
    private ProblemDto problem;
    @Validate
    @ApiModelProperty("OJ附加内容")
    private OjProblemDto ojProblem;
    @Validate
    @ApiModelProperty("选择题 填空题答案")
    private List<ChoiceFillAnswersDto> choices;
    @Validate
    @ApiModelProperty("OJ问题测试用例")
    private List<OjProblemCase> cases;
}
