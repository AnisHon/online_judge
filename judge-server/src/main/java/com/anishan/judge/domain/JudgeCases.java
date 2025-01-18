package com.anishan.judge.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class JudgeCases {

    /**
     * 总分
     */
    private BigDecimal totalScore;

    /**
     * 每个Case内容
     */
    private List<CaseContent> caseContents;


}
