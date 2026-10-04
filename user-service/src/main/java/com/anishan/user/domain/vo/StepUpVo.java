package com.anishan.user.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString(exclude = "stepUpToken")
public class StepUpVo {
    private final String stepUpToken;
    private final long expiresIn;
}
