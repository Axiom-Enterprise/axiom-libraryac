package com.github.axiom.ac.detect.heuristic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ViolationLevelTest {

    private static final double EPS = 1e-9;

    private final ViolationLevel level = new ViolationLevel();

    @Test
    void startsAtZero() {
        assertEquals(0.0, level.level(), EPS);
    }

    @Test
    void addAccumulates() {
        level.add(2.0);
        level.add(3.0);
        assertEquals(5.0, level.level(), EPS);
    }

    @Test
    void decayFloorsAtZero() {
        level.add(3.0);
        level.decay(1.0);
        assertEquals(2.0, level.level(), EPS);
        level.decay(10.0);
        assertEquals(0.0, level.level(), EPS);
    }

    @Test
    void resetClears() {
        level.add(5.0);
        level.reset();
        assertEquals(0.0, level.level(), EPS);
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThrows(IllegalArgumentException.class, () -> level.add(-1.0));
        assertThrows(IllegalArgumentException.class, () -> level.decay(-1.0));
    }
}
