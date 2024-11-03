package com.anishan.problem.domain.dto;

import com.anishan.problem.domain.JudgeAnswer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@ApiModel("题目上传")
public class JudgeRequest {

    @NotNull
    @ApiModelProperty("题目ID")
    private Long problemId;

    @ApiModelProperty("如果是比赛这个字段必填")
    private Long contestId;

    @ApiModelProperty("代码")
    private String code;

    @ApiModelProperty("OJ题目需要指定语言ID")
    private Long languageId;

    @ApiModelProperty("填空选择答案")
    private List<JudgeAnswer> answers;


}
