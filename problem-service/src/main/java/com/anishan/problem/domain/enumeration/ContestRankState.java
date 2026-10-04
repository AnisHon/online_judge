package com.anishan.problem.domain.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

public enum ContestRankState implements IEnum<String> {
    WAITING("WAITING"),
    BUILDING("BUILDING"),
    READY("READY"),
    ERROR("ERROR");

    @EnumValue
    private final String value;

    ContestRankState(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
