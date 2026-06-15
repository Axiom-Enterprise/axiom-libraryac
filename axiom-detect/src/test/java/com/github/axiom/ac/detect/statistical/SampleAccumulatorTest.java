package com.github.axiom.ac.detect.statistical;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SampleAccumulatorTest {

    private static final double EPS = 1e-9;

    private final SampleAccumulator accumulator = new SampleAccumulator(2);
    private final UUID id = UUID.randomUUID();

    @Test
    void recordGrowsThenSlidesAtCapacity() {
        assertArrayEquals(new double[] {1}, accumulator.record(id, 1), EPS);
        assertArrayEquals(new double[] {1, 2}, accumulator.record(id, 2), EPS);
        assertArrayEquals(new double[] {2, 3}, accumulator.record(id, 3), EPS);
    }

    @Test
    void meanEmptyUntilFirstSample() {
        assertTrue(accumulator.mean(id).isEmpty());
        accumulator.record(id, 4);
        accumulator.record(id, 6);
        assertEquals(5.0, accumulator.mean(id).orElseThrow(), EPS);
    }

    @Test
    void perPlayerWindowsAreIndependent() {
        UUID other = UUID.randomUUID();
        accumulator.record(id, 1);
        accumulator.record(other, 9);
        assertEquals(1.0, accumulator.mean(id).orElseThrow(), EPS);
        assertEquals(9.0, accumulator.mean(other).orElseThrow(), EPS);
    }

    @Test
    void forgetAndReset() {
        accumulator.record(id, 1);
        accumulator.forget(id);
        assertTrue(accumulator.mean(id).isEmpty());
        accumulator.record(id, 2);
        accumulator.reset();
        assertTrue(accumulator.mean(id).isEmpty());
    }

    @Test
    void rejectsNonPositiveWindow() {
        assertThrows(IllegalArgumentException.class, () -> new SampleAccumulator(0));
    }
}
