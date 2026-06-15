package com.github.axiom.ac.detect.statistical;

import com.github.axiom.ac.math.Outliers;
import java.util.OptionalDouble;

/**
 * Scores the most recent sample against the Tukey IQR fences of the
 * window. A sample inside the fences scores 0; one outside scores how
 * far past the fence it lies, in interquartile ranges. Unlike a
 * z-score this is robust to the very outliers it hunts, since the
 * quartiles barely move when one sample is extreme.
 */
public final class IqrCriterion implements StatisticalCriterion {

    private static final double EPSILON = 1.0e-9;

    private final int minSamples;

    /**
     * @param minSamples samples required before scoring (at least 4,
     *                   so the quartiles are meaningful)
     */
    public IqrCriterion(int minSamples) {
        if (minSamples < 4) {
            throw new IllegalArgumentException("minSamples must be >= 4");
        }
        this.minSamples = minSamples;
    }

    @Override
    public OptionalDouble score(double[] samples) {
        if (samples.length < minSamples) {
            return OptionalDouble.empty();
        }
        double[] bounds = Outliers.iqrBounds(samples);
        double lower = bounds[0];
        double upper = bounds[1];
        double latest = samples[samples.length - 1];

        double excess = Math.max(0.0, Math.max(lower - latest, latest - upper));
        // The fences span 4 IQRs; recover one IQR to normalise the
        // overshoot into a scale-free score.
        double iqr = (upper - lower) / 4.0;
        return OptionalDouble.of(excess / Math.max(iqr, EPSILON));
    }
}
