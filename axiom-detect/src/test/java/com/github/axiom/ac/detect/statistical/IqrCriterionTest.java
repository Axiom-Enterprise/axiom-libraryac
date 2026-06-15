package com.github.axiom.ac.detect.statistical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IqrCriterionTest {

    private static final double EPS = 1e-9;

    private final IqrCriterion criterion = new IqrCriterion(4);

    @Test
    void emptyBelowMinimumSamples() {
        assertTrue(criterion.score(new double[] {1, 2, 3}).isEmpty());
    }

    @Test
    void zeroWhenLatestSitsInsideTheFences() {
        // Fences of {1..8} are [-2.5, 11.5]; the latest (8) is well inside.
        assertEquals(0.0, criterion.score(new double[] {1, 2, 3, 4, 5, 6, 7, 8}).orElseThrow(), EPS);
    }

    @Test
    void positiveWhenLatestEscapesTheFences() {
        double score = criterion.score(new double[] {1, 2, 3, 4, 5, 6, 7, 100}).orElseThrow();
        assertTrue(score > 10.0);
    }

    @Test
    void rejectsMinimumBelowFour() {
        assertThrows(IllegalArgumentException.class, () -> new IqrCriterion(3));
    }
}
