package com.anishan.problem.domain.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;

    @ApiModelProperty("如果是比赛这个字段必填")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long contestId;

    @ApiModelProperty("代码")
    private String code;

    @ApiModelProperty("OJ题目需要指定语言ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long languageId;

    @ApiModelProperty("填空选择答案")
    private List<JudgeAnswer> answers;

}
