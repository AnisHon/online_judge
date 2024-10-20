package com.anishan.user.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.NotNull;

@Data
public class PasswordResetRequest {

    @NotNull
    @ApiModelProperty(value = "邮箱验证码", required = true)
    private String code;

    @Length(min = 8, max = 16)
    @ApiModelProperty(value = "新密码", required = true)
    private String password;
}
