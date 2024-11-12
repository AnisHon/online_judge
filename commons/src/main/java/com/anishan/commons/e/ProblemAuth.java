package com.anishan.commons.e;

import cn.hutool.core.thread.ThreadUtil;
import com.anishan.commons.util.ThrowUtil;
import com.baomidou.mybatisplus.annotation.IEnum;

public enum ProblemAuth implements Enumerator<Integer>, IEnum<Integer> {
    Public(1),
    Contest(2),
    ;
    private final int value;

    ProblemAuth(int value) {
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
