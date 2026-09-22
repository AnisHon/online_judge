package com.anishan.user.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 个人主页公开资料。这里刻意不包含邮箱、权限、备注和任何认证信息。
 */
@Data
@ApiModel("个人主页公开资料")
public class UserProfileVo {

    @ApiModelProperty("用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("昵称")
    private String nikeName;

    @ApiModelProperty("个性签名")
    private String signature;

    @ApiModelProperty("积分")
    private BigDecimal points;

    @ApiModelProperty("注册时间")
    private LocalDateTime createTime;

    @ApiModelProperty("最近登录时间")
    private LocalDateTime lastLoginTime;
}
