package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RotationTest {

    private static final double EPS = 1e-9;

    @Test
    void wrapDegreesFoldsIntoSignedHalfTurn() {
        assertEquals(-170.0, Rotation.wrapDegrees(190.0), EPS);
        assertEquals(170.0, Rotation.wrapDegrees(-190.0), EPS);
        assertEquals(10.0, Rotation.wrapDegrees(370.0), EPS);
        assertEquals(0.0, Rotation.wrapDegrees(360.0), EPS);
    }

    @Test
    void yawDeltaTakesTheShortestSignedTurn() {
        // 10 -> 350 is a 20-degree turn the short way, not 340.
        assertEquals(-20.0, new Rotation(10, 0).yawDelta(new Rotation(350, 0)), EPS);
        assertEquals(15.0, new Rotation(0, 0).yawDelta(new Rotation(15, 0)), EPS);
    }

    @Test
    void pitchDeltaIsTheSignedDifference() {
        assertEquals(25.0, new Rotation(0, -10).pitchDelta(new Rotation(0, 15)), EPS);
    }

    @Test
    void magnitudeDeltaCombinesYawAndPitch() {
        // Yaw delta 3, pitch delta 4 -> magnitude 5.
        assertEquals(5.0, new Rotation(0, 0).magnitudeDelta(new Rotation(3, 4)), EPS);
    }

    @Test
    void directionVectorFacesPositiveZAtZeroYaw() {
        Vec3 dir = new Rotation(0, 0).directionVector();
        assertEquals(0.0, dir.x(), EPS);
        assertEquals(0.0, dir.y(), EPS);
        assertEquals(1.0, dir.z(), EPS);
    }

    @Test
    void directionVectorFacesNegativeXAtYawNinety() {
        Vec3 dir = new Rotation(90, 0).directionVector();
        assertEquals(-1.0, dir.x(), EPS);
        assertEquals(0.0, dir.z(), EPS);
    }

    @Test
    void directionVectorIsAUnitVector() {
        Vec3 dir = new Rotation(37, -22).directionVector();
        assertEquals(1.0, dir.length(), EPS);
    }

    @Test
    void angleToIsZeroForTheSameRotation() {
        assertEquals(0.0, new Rotation(45, 10).angleTo(new Rotation(45, 10)), EPS);
    }

    @Test
    void angleToIsStraightForOppositeRotations() {
        assertEquals(180.0, new Rotation(0, 0).angleTo(new Rotation(180, 0)), 1e-6);
    }

    @Test
    void angleToIsSymmetric() {
        Rotation a = new Rotation(20, 5);
        Rotation b = new Rotation(95, -30);
        assertTrue(Math.abs(a.angleTo(b) - b.angleTo(a)) < EPS);
    }

    @Test
    void normalizedWrapsYawAndClampsPitch() {
        Rotation canonical = new Rotation(450, 130).normalized();
        assertEquals(90.0, canonical.yaw(), EPS);
        assertEquals(90.0, canonical.pitch(), EPS);
    }

    @Test
    void normalizedKeepsAnInRangeAngle() {
        Rotation inRange = new Rotation(45, -20);
        assertEquals(inRange, inRange.normalized());
    }
}
