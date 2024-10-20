package com.anishan.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;

@Data
@ApiModel("申请发送邮件")
public class EmailCodeRequest {

    @ApiModelProperty("验证码")
    private String captchaCode;

    @ApiModelProperty("验证码Token")
    private String captchaToken;

    @Email
    @ApiModelProperty("邮箱")
    private String email;

}
