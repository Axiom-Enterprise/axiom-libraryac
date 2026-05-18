package com.github.axiom.ac.math;

import java.util.List;

/**
 * Aim-pattern analysis over a sequence of {@link Rotation} samples,
 * oldest first — typically the rotation history of one player.
 *
 * <p>It turns a rotation history into the per-tick delta sequences a
 * check reasons about: signed yaw and pitch steps and the combined
 * angular change. {@link #quantizationGcd} exposes the discrete step
 * a real mouse imposes — legitimate aiming moves in whole multiples
 * of a sensitivity-derived unit, while many aim assists insert
 * fractional corrections that collapse that common divisor.
 *
 * <p>Pair this with {@link Stats} and {@link Outliers} for variance
 * and threshold work on the returned sequences.
 */
public final class AimAnalysis {

    private AimAnalysis() {
    }

    /**
     * Signed, wrapped yaw change between each pair of consecutive
     * samples. The result has one fewer element than {@code samples}.
     */
    public static double[] yawDeltas(List<Rotation> samples) {
        requireSequence(samples);
        double[] deltas = new double[samples.size() - 1];
        for (int i = 0; i < deltas.length; i++) {
            deltas[i] = samples.get(i).yawDelta(samples.get(i + 1));
        }
        return deltas;
    }

    /**
     * Signed pitch change between each pair of consecutive samples.
     * The result has one fewer element than {@code samples}.
     */
    public static double[] pitchDeltas(List<Rotation> samples) {
        requireSequence(samples);
        double[] deltas = new double[samples.size() - 1];
        for (int i = 0; i < deltas.length; i++) {
            deltas[i] = samples.get(i).pitchDelta(samples.get(i + 1));
        }
        return deltas;
    }

    /**
     * Combined angular change between each pair of consecutive
     * samples — always non-negative. The result has one fewer
     * element than {@code samples}.
     */
    public static double[] angularChanges(List<Rotation> samples) {
        requireSequence(samples);
        double[] changes = new double[samples.size() - 1];
        for (int i = 0; i < changes.length; i++) {
            changes[i] = samples.get(i).magnitudeDelta(samples.get(i + 1));
        }
        return changes;
    }

    /** The largest single angular change across {@code samples}. */
    public static double maxAngularChange(List<Rotation> samples) {
        return Stats.max(angularChanges(samples));
    }

    /**
     * True when any consecutive angular change reaches
     * {@code snapDegrees} — an abrupt jump consistent with a rotation
     * snapped onto a target rather than swept toward it.
     */
    public static boolean hasSnap(List<Rotation> samples, double snapDegrees) {
        return maxAngularChange(samples) >= snapDegrees;
    }

    /**
     * Greatest common divisor of the {@code values} after scaling
     * each by {@code scale} and rounding to a whole number — the
     * discrete step underlying the samples.
     *
     * <p>Apply it to {@link #yawDeltas} or {@link #pitchDeltas}: a
     * large divisor means every step is a clean multiple of one unit
     * (a real mouse at a fixed sensitivity); a divisor that collapses
     * toward the rounding unit means fractional, non-quantised steps
     * were mixed in. Values that round to zero are ignored; the
     * result is 0 when every value does.
     *
     * @param values the delta sequence to analyse
     * @param scale  factor mapping a delta to integer units; must be
     *               positive
     */
    public static long quantizationGcd(double[] values, double scale) {
        if (scale <= 0.0) {
            throw new IllegalArgumentException("scale must be positive");
        }
        if (values.length == 0) {
            throw new IllegalArgumentException("values must not be empty");
        }
        long result = 0L;
        for (double value : values) {
            long scaled = Math.round(value * scale);
            if (scaled != 0L) {
                result = Gcd.gcd(result, scaled);
            }
        }
        return result;
    }

    private static void requireSequence(List<Rotation> samples) {
        if (samples.size() < 2) {
            throw new IllegalArgumentException(
                    "need at least 2 rotation samples, got " + samples.size());
        }
    }
}
