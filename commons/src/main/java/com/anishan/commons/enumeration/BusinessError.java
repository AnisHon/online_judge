package com.anishan.commons.enumeration;

import lombok.Getter;

@Getter
public enum BusinessError {

    USERNAME_OR_PASSWORD_WRONG("用户名密码错误");


    private final String msg;

    BusinessError(String msg) {
        this.msg = msg;
    }


}
