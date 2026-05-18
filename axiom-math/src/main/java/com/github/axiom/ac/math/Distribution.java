package com.github.axiom.ac.math;

/**
 * Distribution-shape metrics: population skewness, excess kurtosis,
 * and Shannon entropy. Used to detect artificial regularity in
 * player input (for example automated clicking).
 */
public final class Distribution {

    private Distribution() {
    }

    /** Population skewness. Zero for a symmetric sample. */
    public static double skewness(double[] values) {
        double mean = Stats.mean(values);
        double sd = Stats.standardDeviation(values);
        if (sd == 0.0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            double z = (v - mean) / sd;
            sum += z * z * z;
        }
        return sum / values.length;
    }

    /** Population excess kurtosis. Zero for a normal distribution. */
    public static double kurtosis(double[] values) {
        double mean = Stats.mean(values);
        double sd = Stats.standardDeviation(values);
        if (sd == 0.0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            double z = (v - mean) / sd;
            sum += z * z * z * z;
        }
        return sum / values.length - 3.0;
    }

    /**
     * Shannon entropy, in bits, over category counts. Categories with
     * a zero count are ignored. Throws when the total count is zero.
     */
    public static double entropy(long[] counts) {
        long total = 0;
        for (long c : counts) {
            if (c < 0) {
                throw new IllegalArgumentException("counts must not be negative");
            }
            total += c;
        }
        if (total == 0) {
            throw new IllegalArgumentException("counts must not all be zero");
        }
        double entropy = 0.0;
        for (long c : counts) {
            if (c > 0) {
                double p = (double) c / total;
                entropy -= p * (Math.log(p) / Math.log(2.0));
            }
        }
        return entropy;
    }
}
