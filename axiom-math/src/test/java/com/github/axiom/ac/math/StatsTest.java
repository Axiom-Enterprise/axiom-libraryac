package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class StatsTest {

    private static final double EPS = 1e-9;
    private final double[] sample = {2, 4, 4, 4, 5, 5, 7, 9};

    @Test
    void meanOfSample() {
        assertEquals(5.0, Stats.mean(sample), EPS);
    }

    @Test
    void populationVarianceOfSample() {
        assertEquals(4.0, Stats.variance(sample), EPS);
    }

    @Test
    void standardDeviationOfSample() {
        assertEquals(2.0, Stats.standardDeviation(sample), EPS);
    }

    @Test
    void minAndMax() {
        assertEquals(2.0, Stats.min(sample), EPS);
        assertEquals(9.0, Stats.max(sample), EPS);
    }

    @Test
    void emptyArrayThrows() {
        assertThrows(IllegalArgumentException.class, () -> Stats.mean(new double[0]));
    }
}
