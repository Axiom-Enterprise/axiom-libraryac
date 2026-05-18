package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MotionFormulasTest {

    private static final double EPS = 1e-9;

    @Test
    void verticalVelocityDecaysWithGravityAndDrag() {
        // From rest: (0 - 0.08) * 0.98 = -0.0784.
        assertEquals(-0.0784, MotionFormulas.nextVerticalVelocity(0.0), EPS);
    }

    @Test
    void horizontalFrictionCombinesSlipperinessAndAirFriction() {
        // Default ground: 0.6 * 0.91 = 0.546.
        assertEquals(0.546, MotionFormulas.horizontalFriction(0.6), EPS);
    }

    @Test
    void horizontalVelocityScalesByFriction() {
        assertEquals(0.546, MotionFormulas.nextHorizontalVelocity(1.0, 0.546), EPS);
    }

    @Test
    void jumpVelocityWithoutBoost() {
        assertEquals(0.42, MotionFormulas.jumpVelocity(0), EPS);
    }

    @Test
    void jumpVelocityWithBoost() {
        // Jump Boost II adds 2 * 0.1.
        assertEquals(0.62, MotionFormulas.jumpVelocity(2), EPS);
    }

    @Test
    void negativeJumpBoostThrows() {
        assertThrows(IllegalArgumentException.class, () -> MotionFormulas.jumpVelocity(-1));
    }

    @Test
    void groundAccelerationScalesByInverseCubeOfFriction() {
        // Default surface friction 0.546: 0.1 * 0.21600002 / 0.546^3.
        double expected = 0.1 * 0.21600002 / (0.546 * 0.546 * 0.546);
        assertEquals(expected, MotionFormulas.groundAcceleration(0.546, false), EPS);
    }

    @Test
    void sprintingRaisesGroundAcceleration() {
        double walk = MotionFormulas.groundAcceleration(0.546, false);
        double sprint = MotionFormulas.groundAcceleration(0.546, true);
        assertEquals(walk * 1.3, sprint, EPS);
    }

    @Test
    void slipperierSurfaceLowersGroundAcceleration() {
        double normal = MotionFormulas.groundAcceleration(0.546, false);
        double ice = MotionFormulas.groundAcceleration(0.8918, false);
        assertTrue(ice < normal);
    }

    @Test
    void nonPositiveFrictionThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> MotionFormulas.groundAcceleration(0.0, false));
    }

    @Test
    void airAccelerationIsTheFixedAirValue() {
        assertEquals(0.02, MotionFormulas.airAcceleration(false), EPS);
        assertEquals(0.026, MotionFormulas.airAcceleration(true), EPS);
    }

    @Test
    void slowFallingUsesReducedGravity() {
        // From rest: (0 - 0.01) * 0.98 = -0.0098.
        assertEquals(-0.0098, MotionFormulas.nextVerticalVelocitySlowFalling(0.0), EPS);
    }

    @Test
    void fluidVelocityDragsThenSinks() {
        // 1.0 * 0.8 water drag - 0.02 buoyant gravity = 0.78.
        assertEquals(0.78,
                MotionFormulas.nextVerticalVelocityInFluid(1.0, MotionFormulas.WATER_DRAG),
                EPS);
        // From rest the player sinks by the fluid gravity alone.
        assertEquals(-0.02,
                MotionFormulas.nextVerticalVelocityInFluid(0.0, MotionFormulas.WATER_DRAG),
                EPS);
    }

    @Test
    void levitationBlendsTowardAnUpwardTarget() {
        // From rest, level 1: 0 + (0.05 - 0) * 0.2 = 0.01.
        assertEquals(0.01, MotionFormulas.nextVerticalVelocityLevitation(0.0, 1), EPS);
    }

    @Test
    void levitationRejectsNonPositiveLevel() {
        assertThrows(IllegalArgumentException.class,
                () -> MotionFormulas.nextVerticalVelocityLevitation(0.0, 0));
    }
}
