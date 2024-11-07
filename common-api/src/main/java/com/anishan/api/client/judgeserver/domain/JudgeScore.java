package com.anishan.api.client.judgeserver.domain;

import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.api.client.gojudge.enumeration.Status;
import com.anishan.commons.e.JudgeResult;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class JudgeScore {

    private Long problemId;
    private Long userId;
    private Long contestId;
    private BigDecimal score;
    private JudgeResult result;
    private String code;
    private Long languageId;
    // ms
    private Long runtime;
    // kb
    private Long memory;
    private String errorMessage;

    public JudgeScore runtimeSetter(RunResult runResult) {
        if (runResult != null) {
            this.runtime = runResult.getTime() / 1000 / 1000;
        }
        return this;
    }

    public void memorySetter(RunResult runResult) {
        if (runResult != null) {
            this.memory = runResult.getMemory() / 1024 / 1024;
        }
    }
}
