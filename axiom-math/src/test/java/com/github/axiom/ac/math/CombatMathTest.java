package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CombatMathTest {

    private static final double EPS = 1e-9;

    @Test
    void eyePositionRaisesTheFeetByEyeHeight() {
        Vec3 eye = CombatMath.eyePosition(new Vec3(1, 64, 2));
        assertEquals(new Vec3(1, 64 + 1.62, 2), eye);
    }

    @Test
    void playerHitboxIsCentredOnTheFeet() {
        Aabb box = CombatMath.playerHitbox(new Vec3(0, 64, 0));
        assertEquals(-0.3, box.minX(), EPS);
        assertEquals(0.3, box.maxX(), EPS);
        assertEquals(64.0, box.minY(), EPS);
        assertEquals(64.0 + 1.8, box.maxY(), EPS);
    }

    @Test
    void reachDistanceMeasuresEyeToNearestHitboxPoint() {
        Vec3 eye = CombatMath.eyePosition(new Vec3(0, 64, 0));
        Aabb target = CombatMath.playerHitbox(new Vec3(5, 64, 0));
        // Target +X face is at x = 4.7; eye is at x = 0, within the
        // target's vertical span.
        assertEquals(4.7, CombatMath.reachDistance(eye, target), EPS);
    }

    @Test
    void withinReachComparesAgainstTheLimit() {
        Vec3 eye = CombatMath.eyePosition(new Vec3(0, 64, 0));
        Aabb target = CombatMath.playerHitbox(new Vec3(5, 64, 0));
        assertTrue(CombatMath.withinReach(eye, target, 5.0));
        assertFalse(CombatMath.withinReach(eye, target, 4.0));
    }

    @Test
    void lineOfSightHitsAnAimedTarget() {
        Vec3 eye = new Vec3(0, 65, 0);
        Aabb target = CombatMath.playerHitbox(new Vec3(5, 64, 0));
        // Yaw 270 (i.e. -90) looks along +X, pitch 0 stays level.
        Rotation aimed = new Rotation(-90, 0);
        assertEquals(4.7, CombatMath.lineOfSightDistance(eye, aimed, target).orElseThrow(),
                1e-6);
    }

    @Test
    void lineOfSightMissesWhenLookingAway() {
        Vec3 eye = new Vec3(0, 65, 0);
        Aabb target = CombatMath.playerHitbox(new Vec3(5, 64, 0));
        // Looking along -X, away from the +X target.
        Rotation away = new Rotation(90, 0);
        assertTrue(CombatMath.lineOfSightDistance(eye, away, target).isEmpty());
    }

    @Test
    void looksAtRequiresBothAimAndRange() {
        Vec3 eye = new Vec3(0, 65, 0);
        Aabb target = CombatMath.playerHitbox(new Vec3(5, 64, 0));
        Rotation aimed = new Rotation(-90, 0);
        assertTrue(CombatMath.looksAt(eye, aimed, target, 5.0));
        // Aimed, but the target is beyond the distance bound.
        assertFalse(CombatMath.looksAt(eye, aimed, target, 4.0));
        // In range, but not aimed at the target.
        assertFalse(CombatMath.looksAt(eye, new Rotation(90, 0), target, 10.0));
    }
}
