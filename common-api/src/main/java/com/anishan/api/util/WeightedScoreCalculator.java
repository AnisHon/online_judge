package com.anishan.api.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared exact-decimal normalization for weighted contest scores. */
public final class WeightedScoreCalculator {

    private static final int RATIO_SCALE = 8;
    private static final int SCORE_SCALE = 2;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    private WeightedScoreCalculator() {
    }

    public static BigDecimal normalize(BigDecimal earned, BigDecimal fullMark, BigDecimal maxScore) {
        BigDecimal safeEarned = earned == null ? ZERO : earned;
        BigDecimal safeFullMark = fullMark == null ? ZERO : fullMark;
        BigDecimal safeMaxScore = maxScore == null ? ZERO : maxScore;
        if (safeEarned.signum() < 0 || safeFullMark.signum() < 0 || safeMaxScore.signum() < 0) {
            throw new IllegalArgumentException("Weighted score inputs must not be negative");
        }
        if (safeFullMark.signum() == 0 || safeMaxScore.signum() == 0 || safeEarned.signum() == 0) {
            return ZERO.setScale(SCORE_SCALE, RoundingMode.UNNECESSARY);
        }

        BigDecimal ratio = safeEarned.divide(safeFullMark, RATIO_SCALE, RoundingMode.DOWN).min(ONE);
        BigDecimal normalized = ratio.multiply(safeMaxScore).setScale(SCORE_SCALE, RoundingMode.HALF_DOWN);
        if (normalized.compareTo(safeMaxScore) > 0) {
            normalized = safeMaxScore.setScale(SCORE_SCALE, RoundingMode.DOWN);
        }
        return normalized.max(ZERO);
    }
}
