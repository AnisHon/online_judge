package com.anishan.user.domain.dto;

import lombok.Data;
import lombok.ToString;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@ToString(exclude = {"stepUpToken", "captchaCode"})
public class NewEmailCodeRequest {
    @NotBlank @Size(max = 64) private String stepUpToken;
    @NotBlank @Email @Size(max = 255) private String newEmail;
    @NotBlank @Size(max = 128) private String captchaToken;
    @NotBlank @Size(max = 16) private String captchaCode;
}
