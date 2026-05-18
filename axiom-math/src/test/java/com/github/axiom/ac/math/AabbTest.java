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

    @Test
    void closestPointClampsAnOutsidePointToTheNearestFace() {
        assertEquals(new Vec3(1, 0.5, 0), unit.closestPoint(new Vec3(3, 0.5, -2)));
    }

    @Test
    void closestPointReturnsAnInsidePointUnchanged() {
        Vec3 inside = new Vec3(0.25, 0.5, 0.75);
        assertEquals(inside, unit.closestPoint(inside));
    }

    @Test
    void distanceToIsZeroForAPointInside() {
        assertEquals(0.0, unit.distanceTo(new Vec3(0.5, 0.5, 0.5)), 1e-9);
    }

    @Test
    void distanceToMeasuresToTheNearestFace() {
        // 3 units east of the box's +X face.
        assertEquals(3.0, unit.distanceTo(new Vec3(4, 0.5, 0.5)), 1e-9);
    }

    @Test
    void distanceToMeasuresDiagonalCorners() {
        // Offset by (3, 4, 0) from the (1,1,*) corner -> distance 5.
        assertEquals(5.0, unit.distanceTo(new Vec3(4, 5, 0.5)), 1e-9);
    }
}
