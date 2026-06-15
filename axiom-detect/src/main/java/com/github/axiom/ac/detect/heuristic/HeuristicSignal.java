package com.github.axiom.ac.detect.heuristic;

import java.util.Objects;

/**
 * The outcome of one {@link AbstractHeuristicCheck} evaluation: either
 * the tick looked clean, or it failed with a {@code weight} that says
 * how strongly to push up the player's violation level.
 *
 * @param failed whether this tick was suspicious
 * @param weight amount to add to the violation level on failure ({@code 0} on pass)
 * @param detail short human-readable reason, surfaced in the violation
 */
public record HeuristicSignal(boolean failed, double weight, String detail) {

    public HeuristicSignal {
        Objects.requireNonNull(detail, "detail");
        if (weight < 0.0) {
            throw new IllegalArgumentException("weight must be >= 0");
        }
    }

    /** A clean tick: nothing to add. */
    public static HeuristicSignal pass() {
        return new HeuristicSignal(false, 0.0, "");
    }

    /** A suspicious tick carrying {@code weight} and a reason. */
    public static HeuristicSignal fail(double weight, String detail) {
        return new HeuristicSignal(true, weight, detail);
    }
}
