package com.anishan.problem.domain.vo;
import com.anishan.commons.enumeration.ContestAuth;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@ApiModel("比赛表")
@Data
public class ContestVo {

    @ApiModelProperty("参加比赛的人数")
    private Integer joinedNumber;

    @ApiModelProperty("比赛ID")
    private Long contestId;

    @ApiModelProperty("比赛创建者id")
    private Long userId;

    @ApiModelProperty("比赛标题")
    private String title;

    @ApiModelProperty("题单id")
    private Long listId;

    @ApiModelProperty("密码")
    private String pwd;

    @ApiModelProperty("比赛说明")
    private String description;

    @ApiModelProperty("0 公开赛，1为私有赛（访问需要密码）2为白名单模式(未实现)")
    private ContestAuth auth;

    @ApiModelProperty("开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("结束时间")
    private LocalDateTime endTime;

}