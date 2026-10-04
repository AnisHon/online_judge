package com.anishan.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@ToString(exclude = {"stepUpToken", "newEmailCode"})
@ApiModel("邮箱重设申请")
public class EmailResetRequest {
    @NotBlank @Size(max = 64)
    @ApiModelProperty(value = "敏感操作验证凭证", required = true)
    private String stepUpToken;

    @NotBlank @Size(min = 6, max = 6)
    @ApiModelProperty(value = "新邮箱验证码", required = true)
    private String newEmailCode;

    @NotBlank @Size(max = 255)
    @Email
    @ApiModelProperty(value = "新邮箱", required = true)
    private String newEmail;
}
