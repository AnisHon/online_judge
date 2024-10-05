package com.anishan.user.entity.vo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@Api("用于重置密码，邮箱的返回信息类")
public class AuthResultVo {

    @ApiModelProperty("是否成功")
    private boolean success;

    @ApiModelProperty("成功返回：重制成功，未成功返回原因")
    private String message;

}
