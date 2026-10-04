package com.anishan.user.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@ToString(exclude = {"stepUpToken", "password"})
public class PasswordResetRequest {

    @NotBlank
    @Size(max = 64)
    @ApiModelProperty(value = "敏感操作验证凭证", required = true)
    private String stepUpToken;

    @NotBlank
    @Length(min = 8, max = 16)
    @ApiModelProperty(value = "新密码", required = true)
    private String password;
}
