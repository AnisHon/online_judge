package com.anishan.commons.e;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum Difficulty implements Enumerator<Integer>, IEnum<Integer> {

    Unknown(0),
    easy(1),
    medium(2),
    hard(3),
    ;


    private final int value;

    Difficulty(int value) {
        this.value = value;
    }


    @Override
    public Integer value() {
        return value;
    }

    @Override
    public Integer getValue() {
        return value;
    }
}
