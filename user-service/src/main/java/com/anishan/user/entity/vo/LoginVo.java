package com.anishan.user.entity.vo;

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


}
