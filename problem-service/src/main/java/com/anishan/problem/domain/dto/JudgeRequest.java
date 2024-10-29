package com.anishan.problem.domain.dto;

import com.anishan.problem.domain.JudgeAnswer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("题目上传")
public class JudgeRequest {

    @ApiModelProperty("题目ID")
    private Long problemId;

    private Long languageId;

    @ApiModelProperty("答案")
    private List<JudgeAnswer> answers;

    @ApiModelProperty("如果是比赛这个字段就有用")
    private Long contestId;

    @ApiModelProperty("题单Id，如果是用题单这个字段就有用")
    private Long listId;



}
