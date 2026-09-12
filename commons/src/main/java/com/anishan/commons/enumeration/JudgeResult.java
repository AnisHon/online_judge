package com.anishan.commons.enumeration;

import com.baomidou.mybatisplus.annotation.IEnum;
import io.swagger.annotations.ApiModel;

@ApiModel("QUEUE COMPILING RUNNING AC RE WA TLE MLE CE JUDGE_ERROR")
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
    /**
     * 判题基础设施异常。该状态用于兜底，内部原因只写入管理侧日志，不返回给普通用户。
     */
    JUDGE_ERROR("JUDGE_ERROR"),
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
