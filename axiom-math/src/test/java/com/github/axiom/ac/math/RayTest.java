package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class RayTest {

    private static final double EPS = 1e-9;
    private final Aabb box = new Aabb(2, -0.5, -0.5, 3, 0.5, 0.5);

    @Test
    void hitsBoxAhead() {
        Ray ray = new Ray(new Vec3(0, 0, 0), new Vec3(1, 0, 0));
        OptionalDouble t = ray.intersect(box);
        assertTrue(t.isPresent());
        assertEquals(2.0, t.getAsDouble(), EPS);
    }

    @Test
    void missesBoxToTheSide() {
        Ray ray = new Ray(new Vec3(0, 5, 0), new Vec3(1, 0, 0));
        assertFalse(ray.intersect(box).isPresent());
    }

    @Test
    void missesBoxBehind() {
        Ray ray = new Ray(new Vec3(0, 0, 0), new Vec3(-1, 0, 0));
        assertFalse(ray.intersect(box).isPresent());
    }

    @Test
    void originInsideBoxReturnsZeroDistance() {
        Ray ray = new Ray(new Vec3(2.5, 0, 0), new Vec3(1, 0, 0));
        OptionalDouble t = ray.intersect(box);
        assertTrue(t.isPresent());
        assertEquals(0.0, t.getAsDouble(), EPS);
    }
}
