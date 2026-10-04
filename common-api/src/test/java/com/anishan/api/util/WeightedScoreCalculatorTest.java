package com.anishan.api.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeightedScoreCalculatorTest {

    @Test
    void normalizesPartialAndFullScoresWithDecimalArithmetic() {
        assertEquals(new BigDecimal("3.33"), WeightedScoreCalculator.normalize(
                new BigDecimal("1"), new BigDecimal("3"), new BigDecimal("10")));
        assertEquals(new BigDecimal("10.00"), WeightedScoreCalculator.normalize(
                new BigDecimal("3"), new BigDecimal("3"), new BigDecimal("10")));
    }

    @Test
    void zeroDenominatorAndZeroMaximumProduceZero() {
        assertEquals(new BigDecimal("0.00"), WeightedScoreCalculator.normalize(
                BigDecimal.ONE, BigDecimal.ZERO, new BigDecimal("10")));
        assertEquals(new BigDecimal("0.00"), WeightedScoreCalculator.normalize(
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO));
    }

    @Test
    void rejectsNegativeInputsAndNeverRoundsAboveMaximum() {
        assertThrows(IllegalArgumentException.class, () -> WeightedScoreCalculator.normalize(
                new BigDecimal("-1"), BigDecimal.ONE, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> WeightedScoreCalculator.normalize(
                BigDecimal.ONE, new BigDecimal("-1"), BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> WeightedScoreCalculator.normalize(
                BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("-1")));
        assertEquals(new BigDecimal("0.01"), WeightedScoreCalculator.normalize(
                new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("0.019")));
    }
}
