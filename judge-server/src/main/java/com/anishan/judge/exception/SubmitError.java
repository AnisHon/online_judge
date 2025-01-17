package com.anishan.judge.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: Himit_ZH
 * @Date: 2021/4/16 13:52
 * @Description:
 */
@EqualsAndHashCode(callSuper=true)
@Data
public class SubmitError extends Exception {
    private String message;
    private String stdout;
    private String stderr;

    public SubmitError(String message, String stdout, String stderr) {
        super(message);
        this.message = message;
        this.stdout = stdout;
        this.stderr = stderr;
    }
}