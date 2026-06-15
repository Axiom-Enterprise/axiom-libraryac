package com.github.axiom.ac.detect.raytrace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.axiom.ac.math.Vec3;
import org.junit.jupiter.api.Test;

class LookVectorsTest {

    private static final double EPS = 1e-9;

    @Test
    void yawZeroLooksTowardPositiveZ() {
        Vec3 d = LookVectors.direction(0.0f, 0.0f);
        assertEquals(0.0, d.x(), EPS);
        assertEquals(0.0, d.y(), EPS);
        assertEquals(1.0, d.z(), EPS);
    }

    @Test
    void yawNinetyLooksTowardNegativeX() {
        Vec3 d = LookVectors.direction(90.0f, 0.0f);
        assertEquals(-1.0, d.x(), EPS);
        assertEquals(0.0, d.z(), EPS);
    }

    @Test
    void pitchUpAndDownAreVertical() {
        assertEquals(1.0, LookVectors.direction(0.0f, -90.0f).y(), EPS);
        assertEquals(-1.0, LookVectors.direction(0.0f, 90.0f).y(), EPS);
    }

    @Test
    void directionIsAlwaysUnitLength() {
        assertEquals(1.0, LookVectors.direction(37.0f, -12.0f).length(), EPS);
        assertEquals(1.0, LookVectors.direction(-200.0f, 80.0f).length(), EPS);
    }

    @Test
    void eyePositionRaisesTheFeet() {
        Vec3 eye = LookVectors.eyePosition(new Vec3(1, 64, 2), LookVectors.STANDING_EYE_HEIGHT);
        assertEquals(1.0, eye.x(), EPS);
        assertEquals(64.0 + LookVectors.STANDING_EYE_HEIGHT, eye.y(), EPS);
        assertEquals(2.0, eye.z(), EPS);
    }
}
