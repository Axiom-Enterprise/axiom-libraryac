package com.github.axiom.ac.math;

/**
 * Generic scalar normalization primitives.
 *
 * <p>Anticheat checks turn raw, open-ended measurements (a prediction
 * offset in blocks, a rotation delta in degrees, a packet interval in
 * milliseconds) into bounded, comparable scores. The conversions
 * gathered here are pure functions of their inputs so they can be
 * tested in isolation and reused by every check.
 */
public final class Normalizer {

    private Normalizer() {
    }

    /** Clamps {@code value} into the closed interval {@code [min, max]}. */
    public static double clamp(double value, double min, double max) {
        if (min > max) {
            throw new IllegalArgumentException("min must not exceed max");
        }
        return Math.max(min, Math.min(max, value));
    }

    /** Clamps {@code value} into the unit interval {@code [0, 1]}. */
    public static double clampUnit(double value) {
        return clamp(value, 0.0, 1.0);
    }

    /**
     * Linearly maps {@code value} from {@code [min, max]} onto
     * {@code [0, 1]} and clamps the result, so a value at or below
     * {@code min} yields {@code 0} and one at or above {@code max}
     * yields {@code 1}.
     *
     * @param value the measurement to normalize
     * @param min   the measurement mapped to {@code 0}
     * @param max   the measurement mapped to {@code 1}; must exceed {@code min}
     */
    public static double minMax(double value, double min, double max) {
        if (max <= min) {
            throw new IllegalArgumentException("max must exceed min");
        }
        return clampUnit((value - min) / (max - min));
    }

    /**
     * The standard score of {@code value}: how many standard
     * deviations it lies from {@code mean}. A {@code stdDev} of zero
     * yields zero, treating a degenerate distribution as no signal.
     *
     * @param value  the measurement
     * @param mean   the distribution mean
     * @param stdDev the distribution standard deviation; must not be
     *               negative
     */
    public static double zScore(double value, double mean, double stdDev) {
        if (stdDev < 0.0) {
            throw new IllegalArgumentException("stdDev must not be negative");
        }
        return stdDev == 0.0 ? 0.0 : (value - mean) / stdDev;
    }

    /**
     * The logistic squash of {@code value} onto the open interval
     * {@code (0, 1)}: {@code 0} maps to {@code 0.5}, large positive
     * inputs approach {@code 1}, large negative inputs approach
     * {@code 0}.
     */
    public static double sigmoid(double value) {
        return 1.0 / (1.0 + Math.exp(-value));
    }

    /**
     * A soft cheat score in {@code [0, 1]}: the logistic squash of
     * {@code value} centred on {@code midpoint}, with {@code steepness}
     * setting how sharply the score climbs around it. Convenient for
     * turning a raw deviation into a graded confidence.
     *
     * @param value     the raw measurement
     * @param midpoint  the value scored at {@code 0.5}
     * @param steepness slope of the transition; must be positive
     */
    public static double softScore(double value, double midpoint, double steepness) {
        if (steepness <= 0.0) {
            throw new IllegalArgumentException("steepness must be positive");
        }
        return sigmoid((value - midpoint) * steepness);
    }
}
