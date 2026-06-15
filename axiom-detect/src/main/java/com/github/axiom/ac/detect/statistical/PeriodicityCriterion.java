package com.github.axiom.ac.detect.statistical;

import com.github.axiom.ac.math.Gcd;
import java.util.OptionalDouble;

/**
 * Scores the constant period hidden in a window of interval samples.
 * Samples (for example packet or click gaps, in milliseconds) are
 * rounded to whole units and their greatest common divisor is taken;
 * a large GCD means every interval is a multiple of one base period —
 * the fingerprint of a fixed-rate automation loop. The score is that
 * period, so the matching threshold is "smallest suspicious period".
 */
public final class PeriodicityCriterion implements StatisticalCriterion {

    private final int minSamples;

    /**
     * @param minSamples interval samples required before scoring (at least 2)
     */
    public PeriodicityCriterion(int minSamples) {
        if (minSamples < 2) {
            throw new IllegalArgumentException("minSamples must be >= 2");
        }
        this.minSamples = minSamples;
    }

    @Override
    public OptionalDouble score(double[] samples) {
        if (samples.length < minSamples) {
            return OptionalDouble.empty();
        }
        long[] intervals = new long[samples.length];
        for (int i = 0; i < samples.length; i++) {
            intervals[i] = Math.round(samples[i]);
        }
        return OptionalDouble.of(Gcd.gcdOf(intervals));
    }
}
