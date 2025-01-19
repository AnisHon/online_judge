package com.anishan.commons.enumeration;

import com.baomidou.mybatisplus.annotation.IEnum;
import io.swagger.annotations.ApiModel;

@ApiModel("AC RE WA TLE MLE CE")
public enum JudgeResult implements IEnum<String>, Enumerator<String> {

    QUEUE("QUEUE"), //排队中
    COMPILING("compiling"), // 编译中
    RUNNING("running"),
    ACCEPT("AC"),
    RUNTIME_ERROR("RE"),
    WRONG_ANSWER("WA"),
    TIME_LIMIT_EXCEEDED("TLE"),
    MEMORY_LIMIT_EXCEEDED("MLE"),
    COMPILE_ERROR("CE"),
    ;

    private final String value;
    JudgeResult(String msg) {
        value = msg;
    }

    @Override
    public String value() {
        return this.value;
    }

    @Override
    public String getValue() {
        return this.value;
    }
}
