package com.anishan.api.client.judgeserver.domain;

import com.anishan.api.client.problem.domain.vo.OjProblemCaseVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JudgeMessage {
    private Long userId;
    private Long problemId;
    private Long contestId;
    private Long submitId;
    private Long languageId;
    private String code;
    private String language;
    private Long timeLimit;
    private Long memoryLimit;
    private Integer stackLimit;
    private List<OjProblemCaseVo> cases;
    private BigDecimal score;
    private String testInput;
}
