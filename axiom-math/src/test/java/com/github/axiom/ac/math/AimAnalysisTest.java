package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AimAnalysisTest {

    private static final double EPS = 1e-9;

    private static List<Rotation> rotations(float... yawPitchPairs) {
        if (yawPitchPairs.length % 2 != 0) {
            throw new IllegalArgumentException("expected yaw/pitch pairs");
        }
        Rotation[] result = new Rotation[yawPitchPairs.length / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = new Rotation(yawPitchPairs[2 * i], yawPitchPairs[2 * i + 1]);
        }
        return List.of(result);
    }

    @Test
    void yawDeltasAreConsecutiveSignedTurns() {
        double[] deltas = AimAnalysis.yawDeltas(rotations(0, 0, 10, 0, 25, 0));
        assertArrayEquals(new double[] {10.0, 15.0}, deltas, EPS);
    }

    @Test
    void pitchDeltasAreConsecutiveSignedSteps() {
        double[] deltas = AimAnalysis.pitchDeltas(rotations(0, 0, 0, 5, 0, 5));
        assertArrayEquals(new double[] {5.0, 0.0}, deltas, EPS);
    }

    @Test
    void angularChangesCombineYawAndPitch() {
        double[] changes = AimAnalysis.angularChanges(rotations(0, 0, 3, 4));
        assertArrayEquals(new double[] {5.0}, changes, EPS);
    }

    @Test
    void maxAngularChangeFindsTheLargestStep() {
        assertEquals(80.0,
                AimAnalysis.maxAngularChange(rotations(0, 0, 0, 0, 80, 0)), EPS);
    }

    @Test
    void hasSnapTrueWhenAStepReachesTheThreshold() {
        List<Rotation> samples = rotations(0, 0, 2, 0, 82, 0);
        assertTrue(AimAnalysis.hasSnap(samples, 60.0));
        assertFalse(AimAnalysis.hasSnap(samples, 90.0));
    }

    @Test
    void quantizationGcdFindsTheCommonStep() {
        // Steps of 10, 15, 20 share a divisor of 5.
        long gcd = AimAnalysis.quantizationGcd(new double[] {10.0, 15.0, 20.0}, 1.0);
        assertEquals(5L, gcd);
    }

    @Test
    void quantizationGcdIgnoresZeroSteps() {
        long gcd = AimAnalysis.quantizationGcd(new double[] {0.0, 12.0, 0.0, 18.0}, 1.0);
        assertEquals(6L, gcd);
    }

    @Test
    void quantizationGcdScalesFractionalSteps() {
        // 0.15 and 0.30 scaled by 100 -> 15 and 30 -> divisor 15.
        long gcd = AimAnalysis.quantizationGcd(new double[] {0.15, 0.30}, 100.0);
        assertEquals(15L, gcd);
    }

    @Test
    void quantizationGcdRejectsNonPositiveScale() {
        assertThrows(IllegalArgumentException.class,
                () -> AimAnalysis.quantizationGcd(new double[] {1.0}, 0.0));
    }

    @Test
    void deltaMethodsRejectTooFewSamples() {
        List<Rotation> single = rotations(0, 0);
        assertThrows(IllegalArgumentException.class, () -> AimAnalysis.yawDeltas(single));
        assertThrows(IllegalArgumentException.class, () -> AimAnalysis.pitchDeltas(single));
        assertThrows(IllegalArgumentException.class,
                () -> AimAnalysis.angularChanges(single));
    }
}
