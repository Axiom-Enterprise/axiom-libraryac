package com.github.axiom.ac.detect.statistical;

import com.github.axiom.ac.math.Outliers;
import com.github.axiom.ac.math.Stats;
import java.util.OptionalDouble;

/**
 * Scores the most recent sample by how many standard deviations it
 * sits from the window mean. The score is the absolute z-score, so a
 * value far above or below the player's own baseline both register.
 */
public final class ZScoreCriterion implements StatisticalCriterion {

    private final int minSamples;

    /**
     * @param minSamples samples required before scoring (at least 2,
     *                   so a standard deviation exists)
     */
    public ZScoreCriterion(int minSamples) {
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
        double mean = Stats.mean(samples);
        double stdDev = Stats.standardDeviation(samples);
        double latest = samples[samples.length - 1];
        return OptionalDouble.of(Math.abs(Outliers.zScore(latest, mean, stdDev)));
    }
}
