package com.anishan.commons.enumeration;

import lombok.Getter;

@Getter
public enum CaptchaCodeType {
    Shear(0),
    Gif(1),
    Line(2),
    Circle(3),
    ;
    private final Integer type;

    CaptchaCodeType(Integer type) {
        this.type = type;
    }

}
