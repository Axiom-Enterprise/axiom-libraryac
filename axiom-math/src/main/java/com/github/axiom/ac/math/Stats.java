package com.github.axiom.ac.math;

/**
 * Basic descriptive statistics over double samples. {@code variance}
 * and {@code standardDeviation} use the population definition.
 */
public final class Stats {

    private Stats() {
    }

    public static double mean(double[] values) {
        requireNonEmpty(values);
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    public static double variance(double[] values) {
        requireNonEmpty(values);
        double mean = mean(values);
        double sum = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sum += diff * diff;
        }
        return sum / values.length;
    }

    public static double standardDeviation(double[] values) {
        return Math.sqrt(variance(values));
    }

    public static double min(double[] values) {
        requireNonEmpty(values);
        double min = values[0];
        for (double v : values) {
            if (v < min) {
                min = v;
            }
        }
        return min;
    }

    public static double max(double[] values) {
        requireNonEmpty(values);
        double max = values[0];
        for (double v : values) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    private static void requireNonEmpty(double[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("values must not be empty");
        }
    }
}
