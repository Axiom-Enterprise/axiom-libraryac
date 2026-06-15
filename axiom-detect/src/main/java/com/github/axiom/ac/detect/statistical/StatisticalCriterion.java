package com.github.axiom.ac.detect.statistical;

import java.util.OptionalDouble;

/**
 * Turns a window of samples into a single suspicion score — higher
 * means more anomalous. The score's units are criterion-defined (a
 * z-score, a count, a period), so a check pairs a criterion with a
 * threshold that matches it.
 *
 * <p>Returns empty when the window does not yet hold enough samples
 * to judge; the check treats that as "no opinion", not as a pass.
 */
@FunctionalInterface
public interface StatisticalCriterion {

    /**
     * Scores {@code samples} (oldest first). Returns empty when there
     * is too little data to produce a meaningful score.
     */
    OptionalDouble score(double[] samples);
}
