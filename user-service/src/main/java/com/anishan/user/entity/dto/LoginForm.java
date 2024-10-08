package com.anishan.user.entity.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

@Data
@ApiModel("登陆表单")
public class LoginForm {

    @NotNull
    @Length(min = 1, max = 20, message = "不能为空")
    @ApiModelProperty(required = true)
    private String username;

    @Length(min = 8, max = 16)
    @ApiModelProperty(required = true)
    private String password;

    @NotNull
    @ApiModelProperty(value = "图片验证码token", required = true)
    private String token;

    @NotNull
    @ApiModelProperty(value = "图片验证码code", required = true)
    private String captchaCode;


}
