package com.anishan.user.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("用于返回登陆结果")
public class LoginVo {

    @ApiModelProperty("是否登陆成功")
    private boolean success;

    @ApiModelProperty("登陆成功返回登陆成功，登陆失败返回具体原因")
    private String message;

    @ApiModelProperty("登陆成功后的令牌")
    private String token;

    @ApiModelProperty("短期访问令牌，默认30分钟")
    private String accessToken;

    @ApiModelProperty("长期刷新令牌，默认7天")
    private String refreshToken;

    @ApiModelProperty("访问令牌有效期，单位秒")
    private long expiresIn;


}
