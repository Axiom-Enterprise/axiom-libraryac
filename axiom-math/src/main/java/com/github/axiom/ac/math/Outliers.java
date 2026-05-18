package com.github.axiom.ac.math;

import java.util.Arrays;

/**
 * Outlier-detection helpers: z-score, interpolated percentiles, and
 * Tukey IQR fences. Used to build adaptive detection thresholds.
 */
public final class Outliers {

    private Outliers() {
    }

    /** Standard score. Returns 0 when {@code stdDev} is 0. */
    public static double zScore(double value, double mean, double stdDev) {
        if (stdDev == 0.0) {
            return 0.0;
        }
        return (value - mean) / stdDev;
    }

    /**
     * Percentile {@code p} (in [0, 100]) by linear interpolation on a
     * sorted copy of {@code data}.
     */
    public static double percentile(double[] data, double p) {
        if (data.length == 0) {
            throw new IllegalArgumentException("data must not be empty");
        }
        if (p < 0.0 || p > 100.0) {
            throw new IllegalArgumentException("p must be in [0, 100]");
        }
        double[] sorted = data.clone();
        Arrays.sort(sorted);
        double rank = p / 100.0 * (sorted.length - 1);
        int low = (int) Math.floor(rank);
        int high = (int) Math.ceil(rank);
        if (low == high) {
            return sorted[low];
        }
        return sorted[low] + (rank - low) * (sorted[high] - sorted[low]);
    }

    /** Lower and upper Tukey fences: {@code [Q1 - 1.5*IQR, Q3 + 1.5*IQR]}. */
    public static double[] iqrBounds(double[] data) {
        double q1 = percentile(data, 25);
        double q3 = percentile(data, 75);
        double iqr = q3 - q1;
        return new double[] {q1 - 1.5 * iqr, q3 + 1.5 * iqr};
    }

    /** True when {@code value} falls outside the IQR fences of {@code data}. */
    public static boolean isOutlier(double value, double[] data) {
        double[] bounds = iqrBounds(data);
        return value < bounds[0] || value > bounds[1];
    }
}
