package com.anishan.commons.enumeration;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum AnswerType implements IEnum<Integer>, Enumerator<Integer> {

    Equal(0),
    AnyIn(1),
    NotIn(2),
    ;

    private final int i;
    AnswerType(int i) {
        this.i = i;
    }


    @Override
    public Integer value() {
        return i;
    }

    @Override
    public Integer getValue() {
        return value();
    }
}
