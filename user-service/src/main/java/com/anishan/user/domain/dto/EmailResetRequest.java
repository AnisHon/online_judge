package com.anishan.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("邮箱重设申请")
public class EmailResetRequest {
    @NotNull
    @ApiModelProperty(value = "邮箱验证码", required = true)
    private String code;

    @Email
    @ApiModelProperty(value = "新邮箱", required = true)
    private String newEmail;
}
