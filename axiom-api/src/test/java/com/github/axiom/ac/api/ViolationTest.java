package com.github.axiom.ac.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ViolationTest {

    @Test
    void exposesItsFields() {
        Violation v = new Violation("speed", "moved too fast", 1.5, 0.8);
        assertEquals("speed", v.checkId());
        assertEquals("moved too fast", v.description());
        assertEquals(1.5, v.value());
        assertEquals(0.8, v.confidence());
    }

    @Test
    void rejectsNullCheckId() {
        assertThrows(NullPointerException.class,
                () -> new Violation(null, "desc", 1.0, 0.5));
    }

    @Test
    void rejectsNullDescription() {
        assertThrows(NullPointerException.class,
                () -> new Violation("speed", null, 1.0, 0.5));
    }

    @Test
    void rejectsConfidenceBelowZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new Violation("speed", "desc", 1.0, -0.1));
    }

    @Test
    void rejectsConfidenceAboveOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new Violation("speed", "desc", 1.0, 1.1));
    }
}
