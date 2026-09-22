package com.anishan.api.client.judgeserver.domain;

import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.commons.enumeration.JudgeResult;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class JudgeScore {

    private Long submitId;
    /** 仅用于 problem-service 在异步判题完成后安全释放 Redis 锁。 */
    private String submissionLockToken;
    private Long problemId;
    private Long userId;
    private Long contestId;
    private BigDecimal score;
    private JudgeResult result;
    private String code;
    private Long languageId;
    // ms
    private Long runtime;
    // MiB
    private Long memory;
    private String errorMessage;

    /** 仅供管理侧使用，不能映射到用户公开 VO。 */
    private String internalError;

    /** 机器可检索的内部错误码。 */
    private String errorCode;

    // 总数
    private Integer totalCount;

    // 通过数
    private Integer passCount;

    /** 每个测试用例的结果，供 problem-service 写入内部日志表。 */
    private java.util.List<JudgeCaseResult> caseResults;


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
