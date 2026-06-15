package com.github.axiom.ac.detect.heuristic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HeuristicSignalTest {

    @Test
    void passIsCleanAndWeightless() {
        HeuristicSignal signal = HeuristicSignal.pass();
        assertFalse(signal.failed());
        assertEquals(0.0, signal.weight());
    }

    @Test
    void failCarriesWeightAndDetail() {
        HeuristicSignal signal = HeuristicSignal.fail(2.5, "too fast");
        assertTrue(signal.failed());
        assertEquals(2.5, signal.weight());
        assertEquals("too fast", signal.detail());
    }

    @Test
    void rejectsNegativeWeight() {
        assertThrows(IllegalArgumentException.class, () -> new HeuristicSignal(true, -1.0, "x"));
    }

    @Test
    void rejectsNullDetail() {
        assertThrows(NullPointerException.class, () -> new HeuristicSignal(false, 0.0, null));
    }
}
