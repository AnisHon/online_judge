package com.anishan.problem.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel
public class SupplementContestVo {

    @ApiModelProperty("用户Id")
    private Long userId;

    @ApiModelProperty("用户昵称")
    private String nikeName;

    @ApiModelProperty("比赛Id")
    private Long contestId;

    @ApiModelProperty("最迟时间")
    private LocalDateTime deadline;
}
