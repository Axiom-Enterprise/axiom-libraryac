package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DistributionTest {

    private static final double EPS = 1e-9;

    @Test
    void symmetricSampleHasZeroSkewness() {
        double[] symmetric = {1, 2, 3, 4, 5};
        assertEquals(0.0, Distribution.skewness(symmetric), 1e-9);
    }

    @Test
    void rightTailedSampleHasPositiveSkewness() {
        double[] rightTailed = {1, 1, 1, 1, 10};
        assertTrue(Distribution.skewness(rightTailed) > 0);
    }

    @Test
    void twoPointSampleHasNegativeExcessKurtosis() {
        double[] twoPoint = {0, 0, 0, 1, 1, 1};
        assertEquals(-2.0, Distribution.kurtosis(twoPoint), 1e-9);
    }

    @Test
    void uniformCountsGiveMaxEntropy() {
        // Four equally likely categories -> 2 bits.
        assertEquals(2.0, Distribution.entropy(new long[] {5, 5, 5, 5}), EPS);
    }

    @Test
    void singleCategoryGivesZeroEntropy() {
        assertEquals(0.0, Distribution.entropy(new long[] {7, 0, 0}), EPS);
    }

    @Test
    void entropyOfAllZeroCountsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Distribution.entropy(new long[] {0, 0}));
    }
}
