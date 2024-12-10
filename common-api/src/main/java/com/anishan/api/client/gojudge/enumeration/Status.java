package com.anishan.api.client.gojudge.enumeration;

import com.anishan.commons.enumeration.Enumerator;
import lombok.Getter;

@Getter
public enum Status implements Enumerator<String> {
    Accepted ("Accepted"), // 正常情况
    MemoryLimitExceeded ("Memory Limit Exceeded"), // 内存超限
    TimeLimitExceeded ("Time Limit Exceeded"), // 时间超限
    OutputLimitExceeded ("Output Limit Exceeded"), // 输出超限
    FileError ("File Error"), // 文件错误
    NonzeroExitStatus ("Nonzero Exit Status"), // 非 0 退出值
    Signalled ("Signalled"), // 进程被信号终止
    InternalError ("Internal Error"), // 内部错误
    ;
    
    private final String value;

    Status(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
