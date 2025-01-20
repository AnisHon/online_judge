package com.anishan.problem.domain.dto;

import com.anishan.commons.enumeration.ContestAuth;
import com.anishan.commons.enumeration.ContestType;
import com.anishan.commons.enumeration.ValidationGroup;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;


@ApiModel("比赛表")
@Data
public class ContestDto {

    @NotNull(groups = ValidationGroup.Update.class)
    @ApiModelProperty("比赛ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long contestId;

    @ApiModelProperty(hidden = true)
    private Long userId;

    @ApiModelProperty("比赛标题")
    @NotEmpty(groups = ValidationGroup.Insert.class)
    private String title;

    @ApiModelProperty("题单id")
    @JsonSerialize(using = ToStringSerializer.class)
    @NotNull(groups = ValidationGroup.Insert.class)
    private Long listId;

    @ApiModelProperty("比赛说明")
    private String description;

    @ApiModelProperty("0 公开赛，1为私有赛（访问需要密码）2为白名单模式")
    @NotNull(groups = ValidationGroup.Insert.class)
    private ContestAuth auth;

    @ApiModelProperty("设置比赛类型")
    @NotNull(groups = ValidationGroup.Insert.class)
    private ContestType type;

    @ApiModelProperty("比赛密码")
    private String pwd;

    @ApiModelProperty("开始时间")
    @NotNull(groups = ValidationGroup.Insert.class)
    private LocalDateTime startTime;

    @ApiModelProperty("结束时间")
    @NotNull(groups = ValidationGroup.Insert.class)
    private LocalDateTime endTime;

}