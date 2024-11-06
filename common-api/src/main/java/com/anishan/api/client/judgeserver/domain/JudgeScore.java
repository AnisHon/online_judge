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
    // ms
    private Long runtime;
    // kb
    private Long memory;
    private String errorMessage;

    public JudgeScore setRuntime(RunResult runResult) {
        if (runResult != null) {
            this.runtime = runResult.getTime() / 1000 / 1000;
        }
        return this;
    }

    public JudgeScore setMemory(RunResult runResult) {
        if (runResult != null) {
            this.memory = runResult.getMemory() / 1024 / 1024;
        }
        return this;
    }

    public JudgeScore setResult(Status status) {
        switch (status) {
            case Accepted:
                this.result = JudgeResult.Accept;
                break;
            case TimeLimitExceeded:
                this.result = JudgeResult.TimeLimitExceeded;
                break;
            case MemoryLimitExceeded:
                this.result = JudgeResult.MemoryLimitExceeded;
                break;
            case OutputLimitExceeded:
                this.result = JudgeResult.WrongAnswer;
                break;
            case NonzeroExitStatus:
            case FileError:
            case Signalled:
            case InternalError:
                this.result = JudgeResult.RuntimeError;
                break;
        }
        return this;

    }

    public JudgeScore setResult(JudgeResult result) {
        this.result = result;
        return this;
    }
}
