package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OutliersTest {

    private static final double EPS = 1e-9;

    @Test
    void zScoreOfValue() {
        assertEquals(2.0, Outliers.zScore(20, 10, 5), EPS);
    }

    @Test
    void zScoreWithZeroStdDevIsZero() {
        assertEquals(0.0, Outliers.zScore(20, 10, 0), EPS);
    }

    @Test
    void percentileBounds() {
        double[] data = {1, 2, 3, 4, 5};
        assertEquals(1.0, Outliers.percentile(data, 0), EPS);
        assertEquals(3.0, Outliers.percentile(data, 50), EPS);
        assertEquals(5.0, Outliers.percentile(data, 100), EPS);
    }

    @Test
    void iqrBoundsForSample() {
        double[] data = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] bounds = Outliers.iqrBounds(data);
        // Q1 = 2.75, Q3 = 6.25, IQR = 3.5 -> [-2.5, 11.5].
        assertEquals(-2.5, bounds[0], EPS);
        assertEquals(11.5, bounds[1], EPS);
    }

    @Test
    void isOutlierDetectsExtremeValue() {
        double[] data = {10, 11, 12, 13, 14, 15, 16, 17};
        assertTrue(Outliers.isOutlier(100, data));
        assertFalse(Outliers.isOutlier(13, data));
    }
}
