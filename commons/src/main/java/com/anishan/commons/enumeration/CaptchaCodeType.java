package com.anishan.commons.enumeration;

import lombok.Getter;

@Getter
public enum CaptchaCodeType {
    SHEAR(0),
    GIF(1),
    LINE(2),
    CIRCLE(3),
    ;
    private final Integer type;

    CaptchaCodeType(Integer type) {
        this.type = type;
    }

}
