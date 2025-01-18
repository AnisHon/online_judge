package com.anishan.commons.enumeration;

import com.baomidou.mybatisplus.annotation.IEnum;
import io.swagger.annotations.ApiModel;

@ApiModel("AC RE WA TLE MLE CE")
public enum JudgeResult implements IEnum<String>, Enumerator<String> {

    Queue("QUEUE"), //排队中
    Compiling("compiling"), // 编译中
    Running("running"),
    Accept("AC"),
    RuntimeError("RE"),
    WrongAnswer("WA"),
    TimeLimitExceeded("TLE"),
    MemoryLimitExceeded("MLE"),
    CompileError("CE"),
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
