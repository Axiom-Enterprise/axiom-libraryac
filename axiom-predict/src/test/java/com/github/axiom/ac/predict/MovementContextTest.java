package com.github.axiom.ac.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MovementContextTest {

    private static final double EPS = 1e-9;

    @Test
    void noneHasNoEffectsOrElytra() {
        MovementContext none = MovementContext.none();
        assertEquals(1.0, none.speedEffectMultiplier(), EPS);
        assertFalse(none.hasLevitation());
        assertFalse(none.elytra());
    }

    @Test
    void speedEffectRaisesTheMultiplier() {
        MovementContext speedTwo = new MovementContext(0, 1, 0, 0, false, false);
        assertEquals(1.2, speedTwo.speedEffectMultiplier(), EPS);
    }

    @Test
    void slownessEffectLowersTheMultiplier() {
        MovementContext slowness = new MovementContext(0, 0, 2, 0, false, false);
        assertEquals(1.0 - 0.30, slowness.speedEffectMultiplier(), EPS);
    }

    @Test
    void speedMultiplierNeverGoesNegative() {
        MovementContext crippling = new MovementContext(0, 0, 20, 0, false, false);
        assertEquals(0.0, crippling.speedEffectMultiplier(), EPS);
    }

    @Test
    void levitationIsActiveAboveZero() {
        assertTrue(new MovementContext(0, 0, 0, 1, false, false).hasLevitation());
        assertFalse(new MovementContext(0, 0, 0, 0, false, false).hasLevitation());
    }

    @Test
    void rejectsNegativeAmplifiers() {
        assertThrows(IllegalArgumentException.class,
                () -> new MovementContext(-1, 0, 0, 0, false, false));
        assertThrows(IllegalArgumentException.class,
                () -> new MovementContext(0, 0, 0, -1, false, false));
    }

    @Test
    void sixArgFormLeavesFlightStateNeutral() {
        MovementContext context = new MovementContext(0, 0, 0, 0, false, true);
        assertTrue(context.elytra());
        assertFalse(context.fireworkBoost());
        assertFalse(context.hasRiptideLaunch());
        assertEquals(0, context.depthStrider());
    }

    @Test
    void builderSetsTheFlightAndEnchantmentFields() {
        MovementContext context = MovementContext.builder()
                .elytra(true)
                .fireworkBoost(true)
                .riptideLevel(3)
                .depthStrider(2)
                .dolphinsGrace(true)
                .build();
        assertTrue(context.elytra());
        assertTrue(context.fireworkBoost());
        assertTrue(context.hasRiptideLaunch());
        assertEquals(3, context.riptideLevel());
        assertEquals(2, context.depthStrider());
        assertTrue(context.dolphinsGrace());
    }

    @Test
    void rejectsAnOutOfRangeRiptideLevel() {
        assertThrows(IllegalArgumentException.class,
                () -> MovementContext.builder().riptideLevel(4).build());
        assertThrows(IllegalArgumentException.class,
                () -> MovementContext.builder().riptideLevel(-1).build());
    }
}
