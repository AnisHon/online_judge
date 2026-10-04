package com.anishan.user.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("关注关系摘要")
public class FollowSummaryVo {

    @ApiModelProperty("目标用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @ApiModelProperty("当前登录用户是否关注目标用户")
    private boolean following;

    @ApiModelProperty("目标用户的粉丝数")
    private Long followersCount;

    @ApiModelProperty("目标用户关注的人数")
    private Long followingCount;
}
