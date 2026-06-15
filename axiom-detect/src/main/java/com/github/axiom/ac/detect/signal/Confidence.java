package com.github.axiom.ac.detect.signal;

/**
 * Maps a raw detection magnitude onto the {@code [0, 1]} confidence
 * scale a {@link com.github.axiom.ac.api.Violation} requires. A check
 * works in its own units (a violation level, a z-score, a period);
 * these curves turn that into a comparable confidence without each
 * check reinventing the clamping.
 */
public final class Confidence {

    private Confidence() {
    }

    /** Clamps {@code value} into {@code [0, 1]}. */
    public static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    /**
     * A linear ramp: 0 at or below {@code floor}, 1 at or above
     * {@code ceiling}, interpolated between. Use it to grow confidence
     * as a signal climbs from the flag threshold to a saturation point.
     */
    public static double ramp(double value, double floor, double ceiling) {
        if (ceiling <= floor) {
            throw new IllegalArgumentException("ceiling must exceed floor");
        }
        return clamp((value - floor) / (ceiling - floor));
    }

    /**
     * Saturating ratio: {@code value / scale} capped at 1, and 0 for a
     * non-positive value. {@code scale} is the magnitude that should
     * read as full confidence.
     */
    public static double saturating(double value, double scale) {
        if (scale <= 0.0) {
            throw new IllegalArgumentException("scale must be positive");
        }
        if (value <= 0.0) {
            return 0.0;
        }
        return Math.min(1.0, value / scale);
    }
}
