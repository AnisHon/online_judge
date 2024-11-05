package com.anishan.problem.domain.vo;
import com.anishan.api.domain.vo.OjProblemCaseVo;
import com.anishan.api.domain.vo.OjProblemVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("详细问题包括选择填空答案，OJ测试用例，")
public class AdminDetailProblem {
    @ApiModelProperty("问题本体")
    private ProblemVo problem;
    @ApiModelProperty("OJ附加内容")
    private OjProblemVo ojProblem;
    @ApiModelProperty("选择题 填空题答案")
    private List<ChoiceFillAnswersVo> choices;
    @ApiModelProperty("OJ问题测试用例")
    private List<OjProblemCaseVo> cases;
}
