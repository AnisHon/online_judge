package com.anishan.problem.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Immutable receipt of an accepted OJ submission. Contest score comes from the frozen roster. */
@Data
@Accessors(chain = true)
public class ContestSubmissionContext implements Serializable {
    private Long submitId;
    private Long attemptId;
    private Long contestId;
    private Long userId;
    private Long problemId;
    private Long attemptSeq;
    private BigDecimal maxScore;
    private BigDecimal score;
    private LocalDateTime acceptedAt;
}
