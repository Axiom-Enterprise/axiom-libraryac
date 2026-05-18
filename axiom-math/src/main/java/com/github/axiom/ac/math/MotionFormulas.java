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

    private MotionFormulas() {
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
