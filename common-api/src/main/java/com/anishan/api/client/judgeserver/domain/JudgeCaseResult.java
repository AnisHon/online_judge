package com.anishan.api.client.judgeserver.domain;

import com.anishan.commons.enumeration.JudgeResult;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 单个测试用例的判题结果。该对象只在 judge-server 和 problem-service 的内部接口之间传递。
 */
@Data
@Accessors(chain = true)
public class JudgeCaseResult {

    private Long caseId;
    private Integer caseIndex;
    private JudgeResult status;
    private BigDecimal score;
    private Long time;
    private Long memory;

    /** 不对普通用户公开，写入 judge_case_log.internal_error。 */
    private String errorMessage;
}
