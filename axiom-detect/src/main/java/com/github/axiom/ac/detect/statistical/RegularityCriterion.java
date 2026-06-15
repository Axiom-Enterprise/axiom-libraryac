package com.github.axiom.ac.detect.statistical;

import com.github.axiom.ac.math.Distribution;
import com.github.axiom.ac.math.Stats;
import java.util.OptionalDouble;

/**
 * Scores how artificially regular a window of samples is. The samples
 * are binned into equal-width buckets and the Shannon entropy of that
 * histogram is measured; the score is the entropy deficit
 * {@code log2(buckets) - entropy}. Human input spreads across the
 * buckets and scores low; a machine repeating the same value collapses
 * into one bucket and scores high.
 */
public final class RegularityCriterion implements StatisticalCriterion {

    private final int buckets;
    private final int minSamples;

    /**
     * @param buckets    number of histogram bins (at least 2)
     * @param minSamples samples required before scoring (at least {@code buckets})
     */
    public RegularityCriterion(int buckets, int minSamples) {
        if (buckets < 2) {
            throw new IllegalArgumentException("buckets must be >= 2");
        }
        if (minSamples < buckets) {
            throw new IllegalArgumentException("minSamples must be >= buckets");
        }
        this.buckets = buckets;
        this.minSamples = minSamples;
    }

    @Override
    public OptionalDouble score(double[] samples) {
        if (samples.length < minSamples) {
            return OptionalDouble.empty();
        }
        double maxEntropy = Math.log(buckets) / Math.log(2.0);
        double min = Stats.min(samples);
        double max = Stats.max(samples);
        if (max == min) {
            // Every sample identical: zero entropy, maximal regularity.
            return OptionalDouble.of(maxEntropy);
        }
        long[] counts = histogram(samples, min, max);
        return OptionalDouble.of(maxEntropy - Distribution.entropy(counts));
    }

    private long[] histogram(double[] samples, double min, double max) {
        long[] counts = new long[buckets];
        double width = (max - min) / buckets;
        for (double sample : samples) {
            int index = (int) ((sample - min) / width);
            if (index >= buckets) {
                index = buckets - 1;
            }
            counts[index]++;
        }
        return counts;
    }
}
