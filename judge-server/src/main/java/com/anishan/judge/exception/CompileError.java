package com.anishan.judge.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: Himit_ZH
 * @Date: 2021/1/31 00:16
 * @Description:
 */
@EqualsAndHashCode(callSuper=true)
@Data
public class CompileError extends Exception {
    private String message;
    private String stdout;
    private String stderr;

    public CompileError(String message, String stdout, String stderr) {
        super(message);
        this.message = message;
        this.stdout = stdout;
        this.stderr = stderr;
    }
}