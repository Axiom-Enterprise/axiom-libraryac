package com.github.axiom.ac.math;

/**
 * Pure Minecraft player-movement formulas, with no world collision.
 * These are the building blocks of collision-aware physics
 * simulation, kept here so they are testable in isolation.
 *
 * <p>Constants target the 1.21 player movement model.
 */
public final class MotionFormulas {

    /** Downward acceleration applied per tick. */
    public static final double GRAVITY = 0.08;

    /** Per-tick multiplier applied to vertical velocity. */
    public static final double VERTICAL_DRAG = 0.98;

    /** Base horizontal air friction multiplier. */
    public static final double AIR_FRICTION = 0.91;

    /** Block slipperiness of an ordinary (non-ice) block. */
    public static final double DEFAULT_SLIPPERINESS = 0.6;

    /** Vertical velocity of a jump with no Jump Boost effect. */
    public static final double BASE_JUMP_VELOCITY = 0.42;

    /** Extra jump velocity per Jump Boost amplifier level. */
    public static final double JUMP_BOOST_PER_LEVEL = 0.1;

    /** Base per-tick walking movement speed (the player's speed attribute). */
    public static final double WALK_SPEED = 0.1;

    /** Multiplier applied to the movement speed while sprinting. */
    public static final double SPRINT_MULTIPLIER = 1.3;

    /**
     * Constant scaling the ground move acceleration against the cube
     * of the surface friction, matching Minecraft's {@code travel}.
     */
    public static final double GROUND_ACCELERATION_CONSTANT = 0.21600002;

    /** Per-tick horizontal acceleration of input while airborne. */
    public static final double AIR_ACCELERATION = 0.02;

    /** Per-axis velocity multiplier (drag) for a player in water. */
    public static final double WATER_DRAG = 0.8;

    /** Per-axis velocity multiplier (drag) for a player in lava. */
    public static final double LAVA_DRAG = 0.5;

    /** Downward acceleration applied per tick inside a fluid. */
    public static final double FLUID_GRAVITY = 0.02;

    /** Per-tick horizontal input acceleration while swimming. */
    public static final double FLUID_ACCELERATION = 0.02;

    /** Reduced downward acceleration while Slow Falling is active. */
    public static final double SLOW_FALLING_GRAVITY = 0.01;

    /** Maximum height a player steps up without jumping. */
    public static final double STEP_HEIGHT = 0.6;

    /** Upward speed of a player climbing a ladder or vine. */
    public static final double CLIMB_UP_SPEED = 0.2;

    /** Greatest downward speed while holding onto a climbable block. */
    public static final double CLIMB_FALL_CLAMP = 0.15;

    /** Horizontal speed clamp while on a climbable block. */
    public static final double CLIMB_HORIZONTAL_CLAMP = 0.15;

    /** Per-axis velocity multipliers for a player caught in a cobweb. */
    public static final double COBWEB_DRAG_HORIZONTAL = 0.25;

    /** Vertical velocity multiplier for a player caught in a cobweb. */
    public static final double COBWEB_DRAG_VERTICAL = 0.05;

    /** Target upward velocity contributed per Levitation level. */
    public static final double LEVITATION_ACCELERATION = 0.05;

    /** Blend factor pulling vertical velocity toward the Levitation target. */
    public static final double LEVITATION_BLEND = 0.2;

    /** Velocity added along the look vector per tick of a firework boost. */
    public static final double FIREWORK_LOOK_GAIN = 0.1;

    /** Look-aligned target speed a firework boost converges toward. */
    public static final double FIREWORK_TARGET_FACTOR = 1.5;

    /** Per-tick blend factor pulling glide velocity toward the firework target. */
    public static final double FIREWORK_CONVERGENCE = 0.5;

    /** Riptide launch speed contributed, along the look vector, per level. */
    public static final double RIPTIDE_SPEED_PER_LEVEL = 0.75;

    /** Highest Riptide enchantment level. */
    public static final int RIPTIDE_MAX_LEVEL = 3;

    /** Upward velocity added per tick by an upward bubble column. */
    public static final double BUBBLE_COLUMN_UP_ACCELERATION = 0.04;

    /** Greatest upward velocity an upward bubble column drives toward. */
    public static final double BUBBLE_COLUMN_UP_MAX = 0.7;

    /** Downward velocity added per tick by a downward bubble column. */
    public static final double BUBBLE_COLUMN_DOWN_ACCELERATION = 0.03;

    /** Greatest downward velocity a downward bubble column drives toward. */
    public static final double BUBBLE_COLUMN_DOWN_MAX = -0.9;

    /** Per-axis horizontal velocity multiplier for a player in powder snow. */
    public static final double POWDER_SNOW_DRAG_HORIZONTAL = 0.9;

    /** Greatest downward speed while sinking through powder snow. */
    public static final double POWDER_SNOW_DESCENT_CLAMP = 0.05;

    /** Highest Depth Strider enchantment level. */
    public static final int DEPTH_STRIDER_MAX_LEVEL = 3;

    /** Water friction a maxed Depth Strider converges the player toward. */
    public static final double DEPTH_STRIDER_TARGET_FRICTION = 0.546;

    /** Horizontal water friction while the Dolphin's Grace effect is active. */
    public static final double DOLPHINS_GRACE_FRICTION = 0.96;

    private MotionFormulas() {
    }

    /**
     * Launch speed, along the look vector, of a Riptide trident
     * release at the given enchantment level: {@code level + 1}
     * quarters of three blocks per tick.
     *
     * @param riptideLevel Riptide level, {@code 1}..{@link #RIPTIDE_MAX_LEVEL}
     */
    public static double riptideLaunchSpeed(int riptideLevel) {
        if (riptideLevel < 1 || riptideLevel > RIPTIDE_MAX_LEVEL) {
            throw new IllegalArgumentException(
                    "riptideLevel must be 1.." + RIPTIDE_MAX_LEVEL);
        }
        return RIPTIDE_SPEED_PER_LEVEL * (1 + riptideLevel);
    }

    /**
     * Horizontal water friction for a player with the given Depth
     * Strider level. Depth Strider blends the ordinary {@link
     * #WATER_DRAG} toward {@link #DEPTH_STRIDER_TARGET_FRICTION}; the
     * effect is halved while airborne, matching Minecraft's
     * {@code travel}.
     *
     * @param depthStrider Depth Strider level ({@code 0} = none)
     * @param onGround     whether the player is supported from below
     */
    public static double depthStriderWaterFriction(int depthStrider, boolean onGround) {
        double level = Math.min(Math.max(depthStrider, 0), DEPTH_STRIDER_MAX_LEVEL);
        if (!onGround) {
            level *= 0.5;
        }
        return WATER_DRAG + (DEPTH_STRIDER_TARGET_FRICTION - WATER_DRAG)
                * level / DEPTH_STRIDER_MAX_LEVEL;
    }

    /**
     * Horizontal water input acceleration for a player with the given
     * Depth Strider level. Depth Strider blends the ordinary {@link
     * #FLUID_ACCELERATION} toward the walking move speed; the effect
     * is halved while airborne.
     *
     * @param depthStrider Depth Strider level ({@code 0} = none)
     * @param onGround     whether the player is supported from below
     */
    public static double depthStriderWaterAcceleration(int depthStrider,
                                                       boolean onGround) {
        double level = Math.min(Math.max(depthStrider, 0), DEPTH_STRIDER_MAX_LEVEL);
        if (!onGround) {
            level *= 0.5;
        }
        return FLUID_ACCELERATION + (WALK_SPEED - FLUID_ACCELERATION)
                * level / DEPTH_STRIDER_MAX_LEVEL;
    }

    /**
     * Vertical velocity after one tick of reduced Slow Falling
     * gravity and the usual drag.
     */
    public static double nextVerticalVelocitySlowFalling(double velocityY) {
        return (velocityY - SLOW_FALLING_GRAVITY) * VERTICAL_DRAG;
    }

    /**
     * Vertical velocity after one tick inside a fluid: the velocity
     * is dragged toward zero and then nudged down by buoyant gravity.
     *
     * @param velocityY current vertical velocity
     * @param drag      fluid drag ({@link #WATER_DRAG} or {@link #LAVA_DRAG})
     */
    public static double nextVerticalVelocityInFluid(double velocityY, double drag) {
        return velocityY * drag - FLUID_GRAVITY;
    }

    /**
     * Vertical velocity after one tick of Levitation: the velocity is
     * blended toward an upward target proportional to the effect
     * level.
     *
     * @param velocityY        current vertical velocity
     * @param levitationLevel  Levitation level; must be positive
     */
    public static double nextVerticalVelocityLevitation(double velocityY,
                                                        int levitationLevel) {
        if (levitationLevel <= 0) {
            throw new IllegalArgumentException("levitationLevel must be positive");
        }
        double target = LEVITATION_ACCELERATION * levitationLevel;
        return velocityY + (target - velocityY) * LEVITATION_BLEND;
    }

    /**
     * Per-tick horizontal input acceleration for a player on the
     * ground. Minecraft scales the move speed by the inverse cube of
     * the surface friction, so a slippery block (high friction
     * multiplier) yields a smaller per-tick acceleration but retains
     * speed longer.
     *
     * @param friction effective horizontal friction (slipperiness
     *                 times {@link #AIR_FRICTION}); must be positive
     * @param sprint   whether the player is sprinting
     */
    public static double groundAcceleration(double friction, boolean sprint) {
        if (friction <= 0.0) {
            throw new IllegalArgumentException("friction must be positive");
        }
        double speed = WALK_SPEED * (sprint ? SPRINT_MULTIPLIER : 1.0);
        return speed * GROUND_ACCELERATION_CONSTANT / (friction * friction * friction);
    }

    /**
     * Per-tick horizontal input acceleration for a player in the air.
     * Airborne acceleration is a small fixed value, independent of
     * any surface.
     *
     * @param sprint whether the player is sprinting
     */
    public static double airAcceleration(boolean sprint) {
        return AIR_ACCELERATION * (sprint ? SPRINT_MULTIPLIER : 1.0);
    }

    /** Vertical velocity after one tick of gravity and drag. */
    public static double nextVerticalVelocity(double velocityY) {
        return (velocityY - GRAVITY) * VERTICAL_DRAG;
    }

    /**
     * Effective horizontal friction multiplier for a block of the
     * given {@code slipperiness}.
     */
    public static double horizontalFriction(double slipperiness) {
        return slipperiness * AIR_FRICTION;
    }

    /** Horizontal velocity after one tick of the given friction. */
    public static double nextHorizontalVelocity(double velocity, double friction) {
        return velocity * friction;
    }

    /**
     * Initial vertical velocity of a jump with the given Jump Boost
     * amplifier level (0 = no effect).
     */
    public static double jumpVelocity(int jumpBoostLevel) {
        if (jumpBoostLevel < 0) {
            throw new IllegalArgumentException("jumpBoostLevel must be >= 0");
        }
        return BASE_JUMP_VELOCITY + JUMP_BOOST_PER_LEVEL * jumpBoostLevel;
    }
}
