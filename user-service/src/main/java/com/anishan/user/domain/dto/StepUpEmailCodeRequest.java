package com.anishan.user.domain.dto;

import lombok.Data;
import lombok.ToString;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@ToString(exclude = "captchaCode")
public class StepUpEmailCodeRequest {
    @NotNull private StepUpAction action;
    @NotBlank @Size(max = 128) private String captchaToken;
    @NotBlank @Size(max = 16) private String captchaCode;
}
