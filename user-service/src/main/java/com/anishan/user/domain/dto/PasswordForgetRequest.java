package com.anishan.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

@Data
@ApiModel("密码发送验证码申请")
public class PasswordForgetRequest {

    @NotNull
    @ApiModelProperty(value = "邮箱或者用户名", required = true)
    private String username;

    @NotNull
    @ApiModelProperty(value = "邮箱验证码", required = true)
    private String code;

    @Length(min = 8, max = 16)
    @ApiModelProperty(value = "新密码", required = true)
    private String password;
}
