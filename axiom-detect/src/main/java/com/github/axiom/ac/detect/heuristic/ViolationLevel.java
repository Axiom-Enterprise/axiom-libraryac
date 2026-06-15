package com.github.axiom.ac.detect.heuristic;

/**
 * A mutable, non-negative violation level — the running suspicion
 * score a heuristic check keeps per player. Failures push it up;
 * clean ticks let it decay. It never drops below zero, so a player
 * cannot bank credit by behaving for a long stretch.
 *
 * <p>Not thread-safe — one instance belongs to one player and is
 * touched only on that player's packet thread.
 */
public final class ViolationLevel {

    private double level;

    /** Raises the level by {@code amount}. */
    public void add(double amount) {
        if (amount < 0.0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        level += amount;
    }

    /** Lowers the level by {@code amount}, floored at zero. */
    public void decay(double amount) {
        if (amount < 0.0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        level = Math.max(0.0, level - amount);
    }

    /** The current level. */
    public double level() {
        return level;
    }

    /** Clears the level back to zero. */
    public void reset() {
        level = 0.0;
    }
}
