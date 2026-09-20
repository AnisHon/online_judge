package com.anishan.api.client.problem.domain.vo;

import com.anishan.commons.enumeration.JudgeResult;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 普通用户可见的单个测试点结果。
 *
 * <p>这里刻意不包含 caseId、输入输出和判题机内部错误，避免把题目数据或基础设施信息暴露给用户。</p>
 */
@Data
@Accessors(chain = true)
public class SubmitCaseResultVo {

    /** 从 1 开始展示给用户的测试点序号。 */
    private Integer caseIndex;

    private JudgeResult status;

    private BigDecimal score;

    private Long time;

    private Long memory;
}
