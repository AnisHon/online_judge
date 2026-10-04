package com.anishan.user.domain.dto;

import lombok.Data;
import lombok.ToString;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@ToString(exclude = {"password", "code"})
public class StepUpVerifyRequest {
    public enum Method { PASSWORD, EMAIL }

    @NotNull private StepUpAction action;
    @NotNull private Method method;
    @Size(max = 128) private String password;
    @Size(max = 6) private String code;
}
