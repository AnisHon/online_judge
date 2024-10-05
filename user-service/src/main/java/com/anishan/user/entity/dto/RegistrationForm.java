package com.anishan.user.entity.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("注册表单")
public class RegistrationForm {

    @NotNull
    @Length(min = 1, max = 20)
    @ApiModelProperty(required = true)
    private String userName;

    @ApiModelProperty(required = true)
    private String password;

    @Email
    @ApiModelProperty(required = true)
    private String email;

    @Length(min = 1, max = 20)
    @ApiModelProperty(value = "昵称", required = true)
    private String nikeName;

    @NotNull
    @Length
    @ApiModelProperty(value = "邮箱验证码", required = true)
    private String code;

}
