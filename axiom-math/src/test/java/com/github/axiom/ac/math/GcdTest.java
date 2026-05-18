package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GcdTest {

    @Test
    void gcdOfTwoValues() {
        assertEquals(6L, Gcd.gcd(54, 24));
    }

    @Test
    void gcdHandlesNegativeAndZero() {
        assertEquals(6L, Gcd.gcd(-54, 24));
        assertEquals(7L, Gcd.gcd(7, 0));
        assertEquals(0L, Gcd.gcd(0, 0));
    }

    @Test
    void gcdOfArrayFindsCommonPeriod() {
        // Click intervals that are all multiples of 50 ms.
        assertEquals(50L, Gcd.gcdOf(new long[] {100, 150, 200, 50}));
    }

    @Test
    void gcdOfSingleElement() {
        assertEquals(42L, Gcd.gcdOf(new long[] {42}));
    }

    @Test
    void gcdOfEmptyArrayThrows() {
        assertThrows(IllegalArgumentException.class, () -> Gcd.gcdOf(new long[0]));
    }
}
