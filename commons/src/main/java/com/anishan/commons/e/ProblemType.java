package com.anishan.commons.e;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum ProblemType implements IEnum<Integer>, Enumerator<Integer> {

    OJ(1),
    FILL(2),
    CHOICE(3),
    MULTI_CHOICE(4),
    ;


    private final int value;
    ProblemType(int value) {
        this.value = value;
    }



    @Override
    public Integer value() {
        return this.value;
    }

    @Override
    public Integer getValue() {
        return this.value;
    }
}
