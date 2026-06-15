package com.github.axiom.ac.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import org.junit.jupiter.api.Test;

class OffsetNormalizerTest {

    private static final double EPS = 1e-9;

    @Test
    void rejectsANegativeNoiseFloor() {
        assertThrows(IllegalArgumentException.class,
                () -> new OffsetNormalizer(-0.1, 0.5));
    }

    @Test
    void rejectsSaturationBelowTheFloor() {
        assertThrows(IllegalArgumentException.class,
                () -> new OffsetNormalizer(0.5, 0.5));
    }

    @Test
    void offsetWithinTheNoiseFloorScoresZero() {
        OffsetNormalizer normalizer = new OffsetNormalizer(0.03, 0.5);
        assertEquals(0.0, normalizer.score(0.0), EPS);
        assertEquals(0.0, normalizer.score(0.03), EPS);
    }

    @Test
    void offsetAtSaturationScoresOne() {
        OffsetNormalizer normalizer = new OffsetNormalizer(0.03, 0.5);
        assertEquals(1.0, normalizer.score(0.5), EPS);
        assertEquals(1.0, normalizer.score(2.0), EPS);
    }

    @Test
    void scoreRisesMonotonicallyBetweenFloorAndSaturation() {
        OffsetNormalizer normalizer = new OffsetNormalizer(0.0, 1.0);
        assertTrue(normalizer.score(0.25) < normalizer.score(0.75));
    }

    @Test
    void rejectsANegativeOffset() {
        assertThrows(IllegalArgumentException.class,
                () -> OffsetNormalizer.DEFAULT.score(-1.0));
    }

    @Test
    void scoresAPredictionResultByItsOffset() {
        PlayerState predicted = new PlayerState(
                Vec3.ZERO, Vec3.ZERO, 0.0f, false);
        PredictionResult clean = new PredictionResult(
                MovementInput.none(), predicted, 0.01);
        PredictionResult cheating = new PredictionResult(
                MovementInput.none(), predicted, 0.6);

        assertFalse(OffsetNormalizer.DEFAULT.isSuspicious(clean));
        assertTrue(OffsetNormalizer.DEFAULT.isSuspicious(cheating));
        assertEquals(1.0, OffsetNormalizer.DEFAULT.score(cheating), EPS);
    }
}
