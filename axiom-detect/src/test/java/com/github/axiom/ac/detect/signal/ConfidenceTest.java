package com.github.axiom.ac.detect.signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ConfidenceTest {

    private static final double EPS = 1e-9;

    @Test
    void clampBoundsValue() {
        assertEquals(0.0, Confidence.clamp(-0.5), EPS);
        assertEquals(0.4, Confidence.clamp(0.4), EPS);
        assertEquals(1.0, Confidence.clamp(1.5), EPS);
    }

    @Test
    void rampInterpolatesBetweenFloorAndCeiling() {
        assertEquals(0.0, Confidence.ramp(5, 10, 20), EPS);
        assertEquals(0.5, Confidence.ramp(15, 10, 20), EPS);
        assertEquals(1.0, Confidence.ramp(25, 10, 20), EPS);
    }

    @Test
    void rampRejectsNonAscendingBounds() {
        assertThrows(IllegalArgumentException.class, () -> Confidence.ramp(5, 10, 10));
    }

    @Test
    void saturatingIsRatioCappedAtOne() {
        assertEquals(0.0, Confidence.saturating(-1, 10), EPS);
        assertEquals(0.0, Confidence.saturating(0, 10), EPS);
        assertEquals(0.5, Confidence.saturating(5, 10), EPS);
        assertEquals(1.0, Confidence.saturating(20, 10), EPS);
    }

    @Test
    void saturatingRejectsNonPositiveScale() {
        assertThrows(IllegalArgumentException.class, () -> Confidence.saturating(5, 0));
    }
}
