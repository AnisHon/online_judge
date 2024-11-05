package com.anishan.commons.e;

import com.baomidou.mybatisplus.annotation.IEnum;
import io.swagger.annotations.ApiModel;
import lombok.Getter;

@Getter
@ApiModel("AC RE WA TLE MLE CE")
public enum JudgeResult implements IEnum<String>, Enumerator<String> {

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
        return "";
    }
}
