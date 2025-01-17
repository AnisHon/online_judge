package com.anishan.problem.domain.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel("用户答案请求类")
public class UserAnswerRequest {
    @ApiModelProperty("比赛ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long contestId;

    @ApiModelProperty("题目ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;
}
