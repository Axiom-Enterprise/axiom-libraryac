package com.github.axiom.ac.detect.statistical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegularityCriterionTest {

    private static final double EPS = 1e-9;

    @Test
    void emptyBelowMinimumSamples() {
        RegularityCriterion criterion = new RegularityCriterion(4, 8);
        assertTrue(criterion.score(new double[] {1, 2, 3}).isEmpty());
    }

    @Test
    void identicalSamplesScoreMaximalRegularity() {
        // Four buckets -> max entropy log2(4) = 2 bits; identical input has zero entropy.
        RegularityCriterion criterion = new RegularityCriterion(4, 4);
        assertEquals(2.0, criterion.score(new double[] {5, 5, 5, 5}).orElseThrow(), EPS);
    }

    @Test
    void evenlySpreadSamplesScoreZero() {
        // Two buckets, balanced counts -> 1 bit of entropy == max, deficit 0.
        RegularityCriterion criterion = new RegularityCriterion(2, 2);
        assertEquals(0.0, criterion.score(new double[] {0, 1, 0, 1}).orElseThrow(), EPS);
    }

    @Test
    void constructorValidatesArguments() {
        assertThrows(IllegalArgumentException.class, () -> new RegularityCriterion(1, 4));
        assertThrows(IllegalArgumentException.class, () -> new RegularityCriterion(4, 3));
    }
}
