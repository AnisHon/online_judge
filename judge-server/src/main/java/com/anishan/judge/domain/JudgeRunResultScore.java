package com.anishan.judge.domain;

import com.anishan.commons.enumeration.JudgeResult;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 每个样例判题的得分之类的
 */
@Data
@Builder
public class JudgeRunResultScore {

    private Long caseId;

    private BigDecimal score;

    private JudgeResult judgeResult;

    private boolean passed;

    private Long runtime;

    private Long memory;

    private String errorMessage;

    private String internalError;

}
