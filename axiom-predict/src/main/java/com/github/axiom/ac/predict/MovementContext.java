package com.github.axiom.ac.predict;

/**
 * The slowly changing player attributes a prediction depends on but
 * which are not part of a single tick's {@link MovementInput}:
 * active potion effects and whether an elytra is deployed.
 *
 * <p>Effect fields are amplifiers in Minecraft's convention — level 1
 * of an effect is amplifier {@code 0}. {@code jumpBoost} is the level
 * directly (0 = no effect), matching {@link
 * com.github.axiom.ac.math.MotionFormulas#jumpVelocity(int)}.
 *
 * @param jumpBoost         Jump Boost level (0 = none)
 * @param speedAmplifier    Speed effect amplifier (0 = none or level 1)
 * @param slownessAmplifier Slowness effect amplifier (0 = none or level 1)
 * @param levitationLevel   Levitation level (0 = none, 1+ = active)
 * @param slowFalling       whether Slow Falling is active
 * @param elytra            whether an elytra is deployed and gliding
 */
public record MovementContext(int jumpBoost, int speedAmplifier,
                              int slownessAmplifier, int levitationLevel,
                              boolean slowFalling, boolean elytra) {

    public MovementContext {
        requireNonNegative(jumpBoost, "jumpBoost");
        requireNonNegative(speedAmplifier, "speedAmplifier");
        requireNonNegative(slownessAmplifier, "slownessAmplifier");
        requireNonNegative(levitationLevel, "levitationLevel");
    }

    /** The neutral context: no effects, no elytra. */
    public static MovementContext none() {
        return new MovementContext(0, 0, 0, 0, false, false);
    }

    /**
     * Horizontal movement-speed multiplier from the Speed and
     * Slowness effects: {@code +20%} per Speed level, {@code -15%}
     * per Slowness level, never below zero.
     */
    public double speedEffectMultiplier() {
        double multiplier = 1.0 + 0.2 * speedAmplifier - 0.15 * slownessAmplifier;
        return Math.max(multiplier, 0.0);
    }

    /** True when Levitation is active. */
    public boolean hasLevitation() {
        return levitationLevel > 0;
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
    }
}
