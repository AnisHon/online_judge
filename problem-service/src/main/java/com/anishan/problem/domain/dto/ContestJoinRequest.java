package com.anishan.problem.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;


@ApiModel("用户加入比赛表")
@Data
public class ContestJoinRequest {

    @NotNull
    @ApiModelProperty("比赛ID")
    private Long contestId;

    @ApiModelProperty("比赛密码，如果有需要")
    private String password;

}
