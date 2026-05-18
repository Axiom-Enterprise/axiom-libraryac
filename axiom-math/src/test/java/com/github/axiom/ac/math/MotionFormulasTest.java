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

    @Test
    void riptideLaunchSpeedRisesWithLevel() {
        assertEquals(1.5, MotionFormulas.riptideLaunchSpeed(1), 1e-9);
        assertEquals(2.25, MotionFormulas.riptideLaunchSpeed(2), 1e-9);
        assertEquals(3.0, MotionFormulas.riptideLaunchSpeed(3), 1e-9);
    }

    @Test
    void riptideLaunchSpeedRejectsAnOutOfRangeLevel() {
        assertThrows(IllegalArgumentException.class,
                () -> MotionFormulas.riptideLaunchSpeed(0));
        assertThrows(IllegalArgumentException.class,
                () -> MotionFormulas.riptideLaunchSpeed(4));
    }

    @Test
    void depthStriderRaisesWaterFrictionAndAcceleration() {
        double base = MotionFormulas.depthStriderWaterFriction(0, true);
        double maxed = MotionFormulas.depthStriderWaterFriction(3, true);
        assertEquals(MotionFormulas.WATER_DRAG, base, 1e-9);
        assertEquals(MotionFormulas.DEPTH_STRIDER_TARGET_FRICTION, maxed, 1e-9);
        assertTrue(MotionFormulas.depthStriderWaterAcceleration(3, true)
                > MotionFormulas.depthStriderWaterAcceleration(0, true));
    }

    @Test
    void depthStriderEffectIsHalvedWhileAirborne() {
        // Airborne, the effect is weaker, so the friction stays closer
        // to the unenchanted water drag than the grounded value does.
        double grounded = MotionFormulas.depthStriderWaterFriction(3, true);
        double airborne = MotionFormulas.depthStriderWaterFriction(3, false);
        assertTrue(airborne > grounded);
        assertTrue(airborne < MotionFormulas.WATER_DRAG);
    }
}
