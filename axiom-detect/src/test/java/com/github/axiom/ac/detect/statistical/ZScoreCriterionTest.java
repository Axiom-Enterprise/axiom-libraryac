package com.github.axiom.ac.detect.statistical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ZScoreCriterionTest {

    private static final double EPS = 1e-9;

    private final ZScoreCriterion criterion = new ZScoreCriterion(2);

    @Test
    void emptyBelowMinimumSamples() {
        assertTrue(criterion.score(new double[] {5}).isEmpty());
    }

    @Test
    void zeroWhenAllSamplesEqual() {
        assertEquals(0.0, criterion.score(new double[] {5, 5, 5}).orElseThrow(), EPS);
    }

    @Test
    void scoresLatestAgainstTheWindow() {
        // {0, 10}: mean 5, population sd 5, latest 10 -> |z| = 1.
        assertEquals(1.0, criterion.score(new double[] {0, 10}).orElseThrow(), EPS);
    }

    @Test
    void rejectsMinimumBelowTwo() {
        assertThrows(IllegalArgumentException.class, () -> new ZScoreCriterion(1));
    }
}
