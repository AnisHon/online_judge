package com.anishan.commons.e;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

// 0公开赛，1为私有赛（访问需要密码）
@Getter
public enum ContestAuth implements IEnum<Integer>, Enumerator<Integer> {

    PUBLIC(0),
    PRIVATE(1),
    WhiteList(2),
    ;

    private final int value;

    ContestAuth(int value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    @Override
    public Integer value() {
        return value;
    }
}
