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

    private MotionFormulas() {
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
