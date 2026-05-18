package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class Vec3Test {

    private static final double EPS = 1e-9;

    @Test
    void addSubtractScale() {
        Vec3 a = new Vec3(1, 2, 3);
        Vec3 b = new Vec3(4, 5, 6);
        assertEquals(new Vec3(5, 7, 9), a.add(b));
        assertEquals(new Vec3(-3, -3, -3), a.subtract(b));
        assertEquals(new Vec3(2, 4, 6), a.scale(2));
    }

    @Test
    void dotProduct() {
        assertEquals(32.0, new Vec3(1, 2, 3).dot(new Vec3(4, 5, 6)), EPS);
    }

    @Test
    void lengthAndDistance() {
        assertEquals(5.0, new Vec3(3, 4, 0).length(), EPS);
        assertEquals(25.0, new Vec3(3, 4, 0).lengthSquared(), EPS);
        assertEquals(5.0, new Vec3(0, 0, 0).distance(new Vec3(3, 4, 0)), EPS);
    }

    @Test
    void normalizeProducesUnitVector() {
        assertEquals(1.0, new Vec3(0, 0, 7).normalize().length(), EPS);
    }

    @Test
    void normalizeZeroVectorReturnsZero() {
        assertEquals(new Vec3(0, 0, 0), new Vec3(0, 0, 0).normalize());
    }
}
