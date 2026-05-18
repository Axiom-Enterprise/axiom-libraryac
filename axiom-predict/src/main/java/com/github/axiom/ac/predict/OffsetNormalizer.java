package com.github.axiom.ac.predict;

import com.github.axiom.ac.math.Normalizer;

/**
 * Turns a raw {@link PredictionResult#offset()} — the distance, in
 * blocks, between the best-matching prediction and where the client
 * claims to be — into a bounded cheat score in {@code [0, 1]}.
 *
 * <p>A prediction never lands exactly: floating-point drift, the
 * documented constant approximations, and unmodelled micro-mechanics
 * leave a small residual offset on legitimate play. The normalizer
 * absorbs that residual with a {@code noiseFloor}: offsets at or below
 * it score {@code 0}. Past the floor the score rises linearly and
 * reaches {@code 1} at the {@code saturation} offset, beyond which no
 * legitimate input could explain the move.
 *
 * <p>Instances are immutable and thread-safe.
 */
public final class OffsetNormalizer {

    /**
     * A general-purpose normalizer: a 3&nbsp;cm noise floor and
     * saturation at half a block.
     */
    public static final OffsetNormalizer DEFAULT = new OffsetNormalizer(0.03, 0.5);

    private final double noiseFloor;
    private final double saturation;

    /**
     * @param noiseFloor offset at or below which the score is
     *                   {@code 0}; must not be negative
     * @param saturation offset at or above which the score is
     *                   {@code 1}; must exceed {@code noiseFloor}
     */
    public OffsetNormalizer(double noiseFloor, double saturation) {
        if (noiseFloor < 0.0) {
            throw new IllegalArgumentException("noiseFloor must not be negative");
        }
        if (saturation <= noiseFloor) {
            throw new IllegalArgumentException("saturation must exceed noiseFloor");
        }
        this.noiseFloor = noiseFloor;
        this.saturation = saturation;
    }

    /** Offset at or below which a move scores {@code 0}. */
    public double noiseFloor() {
        return noiseFloor;
    }

    /** Offset at or above which a move scores {@code 1}. */
    public double saturation() {
        return saturation;
    }

    /**
     * The cheat score for a raw {@code offset} in blocks: {@code 0} up
     * to the noise floor, rising linearly to {@code 1} at saturation.
     *
     * @param offset the prediction offset; must not be negative
     */
    public double score(double offset) {
        if (offset < 0.0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        return Normalizer.minMax(offset, noiseFloor, saturation);
    }

    /** The cheat score for a {@link PredictionResult}. */
    public double score(PredictionResult result) {
        return score(result.offset());
    }

    /**
     * Whether {@code result}'s offset clears the noise floor — that
     * is, whether the move carries any cheat signal at all.
     */
    public boolean isSuspicious(PredictionResult result) {
        return result.offset() > noiseFloor;
    }
}
