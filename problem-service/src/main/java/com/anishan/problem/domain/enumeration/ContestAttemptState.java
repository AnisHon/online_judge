package com.anishan.problem.domain.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

/** Attempt lifecycle shared by the durable acceptance and result-application pipeline. */
public enum ContestAttemptState implements IEnum<String> {
    PENDING("PENDING"),
    COMPLETED("COMPLETED"),
    INFRA_ERROR("INFRA_ERROR");

    @EnumValue
    private final String value;

    ContestAttemptState(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
