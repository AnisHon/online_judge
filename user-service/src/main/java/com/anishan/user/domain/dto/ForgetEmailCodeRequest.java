package com.anishan.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("申请发送邮件")
public class ForgetEmailCodeRequest {

    @ApiModelProperty("验证码")
    private String captchaCode;

    @ApiModelProperty("验证码Token")
    private String captchaToken;

    @NotNull
    @ApiModelProperty("用户名或邮箱")
    private String username;

}
