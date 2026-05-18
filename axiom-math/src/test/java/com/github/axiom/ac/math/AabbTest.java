package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AabbTest {

    private final Aabb unit = new Aabb(0, 0, 0, 1, 1, 1);

    @Test
    void intersectsOverlappingBox() {
        assertTrue(unit.intersects(new Aabb(0.5, 0.5, 0.5, 2, 2, 2)));
    }

    @Test
    void doesNotIntersectDisjointBox() {
        assertFalse(unit.intersects(new Aabb(2, 2, 2, 3, 3, 3)));
    }

    @Test
    void touchingFacesDoNotIntersect() {
        assertFalse(unit.intersects(new Aabb(1, 0, 0, 2, 1, 1)));
    }

    @Test
    void containsPointInside() {
        assertTrue(unit.contains(new Vec3(0.5, 0.5, 0.5)));
    }

    @Test
    void doesNotContainPointOutside() {
        assertFalse(unit.contains(new Vec3(1.5, 0.5, 0.5)));
    }

    @Test
    void expandGrowsBoxOutward() {
        assertEquals(new Aabb(-1, -1, -1, 2, 2, 2), unit.expand(1, 1, 1));
    }

    @Test
    void offsetTranslatesBox() {
        assertEquals(new Aabb(1, 1, 1, 2, 2, 2), unit.offset(1, 1, 1));
    }
}
