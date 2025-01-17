package com.anishan.judge.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: Himit_ZH
 * @Date: 2021/1/31 00:17
 * @Description:
 */
@EqualsAndHashCode(callSuper=true)
@Data
public class SystemError extends Exception {
    private String message;
    private String stdout;
    private String stderr;

    public SystemError(String message, String stdout, String stderr) {
        super(message + " " + stderr);
        this.message = message;
        this.stdout = stdout;
        this.stderr = stderr;
    }

}