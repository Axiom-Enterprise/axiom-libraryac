package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NormalizerTest {

    private static final double EPS = 1e-9;

    @Test
    void clampBoundsTheValue() {
        assertEquals(0.0, Normalizer.clamp(-5.0, 0.0, 10.0), EPS);
        assertEquals(10.0, Normalizer.clamp(50.0, 0.0, 10.0), EPS);
        assertEquals(4.0, Normalizer.clamp(4.0, 0.0, 10.0), EPS);
    }

    @Test
    void clampRejectsInvertedBounds() {
        assertThrows(IllegalArgumentException.class,
                () -> Normalizer.clamp(1.0, 10.0, 0.0));
    }

    @Test
    void minMaxMapsAndClamps() {
        assertEquals(0.0, Normalizer.minMax(0.0, 2.0, 6.0), EPS);
        assertEquals(0.5, Normalizer.minMax(4.0, 2.0, 6.0), EPS);
        assertEquals(1.0, Normalizer.minMax(99.0, 2.0, 6.0), EPS);
    }

    @Test
    void minMaxRejectsAnEmptyRange() {
        assertThrows(IllegalArgumentException.class,
                () -> Normalizer.minMax(1.0, 5.0, 5.0));
    }

    @Test
    void zScoreCountsStandardDeviations() {
        assertEquals(2.0, Normalizer.zScore(14.0, 10.0, 2.0), EPS);
        assertEquals(-1.5, Normalizer.zScore(7.0, 10.0, 2.0), EPS);
    }

    @Test
    void zScoreOfADegenerateDistributionIsZero() {
        assertEquals(0.0, Normalizer.zScore(42.0, 10.0, 0.0), EPS);
    }

    @Test
    void sigmoidMapsZeroToAHalf() {
        assertEquals(0.5, Normalizer.sigmoid(0.0), EPS);
        assertTrue(Normalizer.sigmoid(8.0) > 0.99);
        assertTrue(Normalizer.sigmoid(-8.0) < 0.01);
    }

    @Test
    void softScoreIsHalfAtTheMidpoint() {
        assertEquals(0.5, Normalizer.softScore(3.0, 3.0, 2.0), EPS);
        assertTrue(Normalizer.softScore(9.0, 3.0, 2.0) > 0.5);
    }

    @Test
    void softScoreRejectsNonPositiveSteepness() {
        assertThrows(IllegalArgumentException.class,
                () -> Normalizer.softScore(1.0, 0.0, 0.0));
    }
}
