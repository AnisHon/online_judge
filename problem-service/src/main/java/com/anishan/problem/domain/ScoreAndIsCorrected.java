package com.anishan.problem.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScoreAndIsCorrected {

    private BigDecimal score;
    private boolean isCorrected;

    public static ScoreAndIsCorrected wrong() {
        return new ScoreAndIsCorrected(BigDecimal.ZERO, false);
    }

    public static ScoreAndIsCorrected corrected(BigDecimal score) {
        return new ScoreAndIsCorrected(score, true);
    }
}
