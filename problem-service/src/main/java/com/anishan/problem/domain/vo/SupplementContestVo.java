package com.anishan.problem.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel
public class SupplementContestVo {

    @ApiModelProperty("用户Id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @ApiModelProperty("用户昵称")
    private String nikeName;

    @ApiModelProperty("比赛Id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long contestId;

    @ApiModelProperty("最迟时间")
    private LocalDateTime deadline;
}
