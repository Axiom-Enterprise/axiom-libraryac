package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlidingWindowTest {

    private static final double EPS = 1e-9;

    @Test
    void tracksValuesUpToCapacity() {
        SlidingWindow window = new SlidingWindow(3);
        window.add(1);
        window.add(2);
        assertEquals(2, window.size());
        assertArrayEquals(new double[] {1, 2}, window.toArray(), EPS);
    }

    @Test
    void slidesPastCapacity() {
        SlidingWindow window = new SlidingWindow(3);
        window.add(1);
        window.add(2);
        window.add(3);
        window.add(4);
        assertTrue(window.isFull());
        assertArrayEquals(new double[] {2, 3, 4}, window.toArray(), EPS);
    }

    @Test
    void computesMeanAndStdDevOverWindow() {
        SlidingWindow window = new SlidingWindow(4);
        window.add(2);
        window.add(4);
        window.add(4);
        window.add(6);
        assertEquals(4.0, window.mean(), EPS);
        assertEquals(Math.sqrt(2.0), window.standardDeviation(), EPS);
    }
}
