package com.github.axiom.ac.predict;

import com.github.axiom.ac.math.MotionFormulas;

/**
 * The slowly changing player attributes a prediction depends on but
 * which are not part of a single tick's {@link MovementInput}: active
 * potion effects, worn enchantments, and the elytra / firework /
 * Riptide state.
 *
 * <p>Effect fields are amplifiers in Minecraft's convention — level 1
 * of an effect is amplifier {@code 0}. {@code jumpBoost} is the level
 * directly (0 = no effect), matching {@link
 * MotionFormulas#jumpVelocity(int)}.
 *
 * <p>{@code fireworkBoost}, {@code riptideLevel}, and {@code elytra}
 * describe transient flight state the anticheat reads from the server:
 * {@code fireworkBoost} is set on every tick a firework rocket is
 * propelling a glide, and {@code riptideLevel} is set on the single
 * tick a Riptide trident launches the player.
 *
 * @param jumpBoost         Jump Boost level (0 = none)
 * @param speedAmplifier    Speed effect amplifier (0 = none or level 1)
 * @param slownessAmplifier Slowness effect amplifier (0 = none or level 1)
 * @param levitationLevel   Levitation level (0 = none, 1+ = active)
 * @param slowFalling       whether Slow Falling is active
 * @param elytra            whether an elytra is deployed and gliding
 * @param fireworkBoost     whether a firework rocket is boosting the
 *                          glide this tick
 * @param riptideLevel      Riptide level launching the player this
 *                          tick (0 = no launch, 1..3 = active)
 * @param depthStrider      Depth Strider enchantment level (0 = none)
 * @param dolphinsGrace     whether the Dolphin's Grace effect is active
 */
public record MovementContext(int jumpBoost, int speedAmplifier,
                              int slownessAmplifier, int levitationLevel,
                              boolean slowFalling, boolean elytra,
                              boolean fireworkBoost, int riptideLevel,
                              int depthStrider, boolean dolphinsGrace) {

    public MovementContext {
        requireNonNegative(jumpBoost, "jumpBoost");
        requireNonNegative(speedAmplifier, "speedAmplifier");
        requireNonNegative(slownessAmplifier, "slownessAmplifier");
        requireNonNegative(levitationLevel, "levitationLevel");
        requireNonNegative(depthStrider, "depthStrider");
        if (riptideLevel < 0 || riptideLevel > MotionFormulas.RIPTIDE_MAX_LEVEL) {
            throw new IllegalArgumentException(
                    "riptideLevel must be 0.." + MotionFormulas.RIPTIDE_MAX_LEVEL);
        }
    }

    /**
     * A context with effects and the elytra flag but no flight-state
     * extras — kept for source compatibility with the pre-1.21-flight
     * six-argument form.
     */
    public MovementContext(int jumpBoost, int speedAmplifier,
                           int slownessAmplifier, int levitationLevel,
                           boolean slowFalling, boolean elytra) {
        this(jumpBoost, speedAmplifier, slownessAmplifier, levitationLevel,
                slowFalling, elytra, false, 0, 0, false);
    }

    /** The neutral context: no effects, no elytra, no flight state. */
    public static MovementContext none() {
        return new MovementContext(0, 0, 0, 0, false, false, false, 0, 0, false);
    }

    /** A fresh builder, initialised to the neutral context. */
    public static Builder builder() {
        return new Builder();
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

    /** True when a Riptide trident launches the player this tick. */
    public boolean hasRiptideLaunch() {
        return riptideLevel > 0;
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
    }

    /**
     * Fluent builder for a {@link MovementContext}, sparing callers
     * from the long positional constructor when only a few attributes
     * are non-neutral.
     */
    public static final class Builder {

        private int jumpBoost;
        private int speedAmplifier;
        private int slownessAmplifier;
        private int levitationLevel;
        private boolean slowFalling;
        private boolean elytra;
        private boolean fireworkBoost;
        private int riptideLevel;
        private int depthStrider;
        private boolean dolphinsGrace;

        private Builder() {
        }

        /** Sets the Jump Boost level. */
        public Builder jumpBoost(int level) {
            this.jumpBoost = level;
            return this;
        }

        /** Sets the Speed effect amplifier. */
        public Builder speedAmplifier(int amplifier) {
            this.speedAmplifier = amplifier;
            return this;
        }

        /** Sets the Slowness effect amplifier. */
        public Builder slownessAmplifier(int amplifier) {
            this.slownessAmplifier = amplifier;
            return this;
        }

        /** Sets the Levitation level. */
        public Builder levitationLevel(int level) {
            this.levitationLevel = level;
            return this;
        }

        /** Sets whether Slow Falling is active. */
        public Builder slowFalling(boolean active) {
            this.slowFalling = active;
            return this;
        }

        /** Sets whether an elytra is deployed and gliding. */
        public Builder elytra(boolean deployed) {
            this.elytra = deployed;
            return this;
        }

        /** Sets whether a firework rocket boosts the glide this tick. */
        public Builder fireworkBoost(boolean boosting) {
            this.fireworkBoost = boosting;
            return this;
        }

        /** Sets the Riptide level launching the player this tick. */
        public Builder riptideLevel(int level) {
            this.riptideLevel = level;
            return this;
        }

        /** Sets the Depth Strider enchantment level. */
        public Builder depthStrider(int level) {
            this.depthStrider = level;
            return this;
        }

        /** Sets whether the Dolphin's Grace effect is active. */
        public Builder dolphinsGrace(boolean active) {
            this.dolphinsGrace = active;
            return this;
        }

        /** Builds the immutable {@link MovementContext}. */
        public MovementContext build() {
            return new MovementContext(jumpBoost, speedAmplifier,
                    slownessAmplifier, levitationLevel, slowFalling, elytra,
                    fireworkBoost, riptideLevel, depthStrider, dolphinsGrace);
        }
    }
}
