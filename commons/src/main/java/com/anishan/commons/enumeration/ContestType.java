package com.anishan.commons.enumeration;


import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum ContestType implements IEnum<Integer>, Enumerator<Integer> {

    CONTEST(0),
    HOMEWORK(1),
    ;

    private final int value;

    ContestType(int value) {
        this.value = value;
    }

    @Override
    public Integer value() {
        return value;
    }

    public Integer getValue() {
        return value;
    }
}