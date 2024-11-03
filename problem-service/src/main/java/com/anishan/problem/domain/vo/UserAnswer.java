package com.anishan.problem.domain.vo;

import com.anishan.problem.domain.JudgeAnswer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@ApiModel("用户答案")
@AllArgsConstructor
@NoArgsConstructor
public class UserAnswer {
    @ApiModelProperty("用户填空选择答案")
    private List<JudgeAnswer> answers;

    @ApiModelProperty("用户代码")
    private String code;

    @ApiModelProperty("编程语言ID")
    private Long languageId;
}
