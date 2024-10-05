package com.anishan.commons.e;


import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum UserState implements Enumerator<Integer>, IEnum<Integer> {

    BANNED(1),
    NORMAL(0);

    private final int value;

    UserState(int value) {
        this.value = value;
    }


    @Override
    public Integer value() {
        return this.value;
    }

    public Integer getValue() {
        return this.value;
    }
}
