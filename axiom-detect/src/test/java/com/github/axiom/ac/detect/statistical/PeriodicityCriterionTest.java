package com.github.axiom.ac.detect.statistical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PeriodicityCriterionTest {

    private static final double EPS = 1e-9;

    private final PeriodicityCriterion criterion = new PeriodicityCriterion(2);

    @Test
    void emptyBelowMinimumSamples() {
        assertTrue(criterion.score(new double[] {50}).isEmpty());
    }

    @Test
    void recoversTheCommonPeriod() {
        // All intervals are multiples of 50 ms.
        assertEquals(50.0, criterion.score(new double[] {50, 100, 150, 200}).orElseThrow(), EPS);
    }

    @Test
    void coprimeIntervalsCollapseToOne() {
        assertEquals(1.0, criterion.score(new double[] {50, 51}).orElseThrow(), EPS);
    }

    @Test
    void roundsSamplesBeforeTakingTheGcd() {
        assertEquals(50.0, criterion.score(new double[] {49.6, 100.2}).orElseThrow(), EPS);
    }

    @Test
    void rejectsMinimumBelowTwo() {
        assertThrows(IllegalArgumentException.class, () -> new PeriodicityCriterion(1));
    }
}
