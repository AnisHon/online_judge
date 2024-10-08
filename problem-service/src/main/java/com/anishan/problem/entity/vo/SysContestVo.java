package com.anishan.problem.entity.vo;

import com.anishan.commons.e.ContestAuth;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel("比赛")
public class SysContestVo {

    @ApiModelProperty("比赛主键")
    private Long contestId;

    @ApiModelProperty("比赛创建者id")
    private Long userId;

    @ApiModelProperty("比赛标题")
    private String title;

    @ApiModelProperty("提单ID")
    private Long listId;

    @ApiModelProperty("比赛说明")
    private String description;

    @ApiModelProperty("0公开赛，1为私有赛（访问需要密码）")
    private ContestAuth auth;

    @ApiModelProperty("比赛密码")
    private String pwd;

    @ApiModelProperty("开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;


}