package com.github.axiom.ac.detect.statistical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.api.PlayerData;
import com.github.axiom.ac.api.Violation;
import com.github.axiom.ac.detect.FakePlayerData;
import com.github.axiom.ac.math.Vec3;
import java.util.OptionalDouble;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AbstractStatisticalCheckTest {

    private static final double EPS = 1e-9;

    /** Criterion that simply reports the most recent sample as the score. */
    private static final StatisticalCriterion LATEST =
            samples -> OptionalDouble.of(samples[samples.length - 1]);

    /** Test check feeding a settable sample into a chosen criterion. */
    private static final class ValueCheck extends AbstractStatisticalCheck {
        private OptionalDouble next = OptionalDouble.empty();

        ValueCheck(StatisticalCriterion criterion, int window, double flag, double saturation) {
            super("stat", criterion, window, flag, saturation);
        }

        @Override
        protected OptionalDouble sample(PlayerData data) {
            return next;
        }
    }

    private final UUID id = UUID.randomUUID();
    private final PlayerData data = FakePlayerData.at(id, new Vec3(0, 0, 0));
    private final ValueCheck check = new ValueCheck(LATEST, 5, 2.0, 6.0);

    @Test
    void skipsWhenNoSampleIsAvailable() {
        check.next = OptionalDouble.empty();
        assertTrue(check.inspect(data).isEmpty());
    }

    @Test
    void quietBelowTheFlagScore() {
        check.next = OptionalDouble.of(1.0);
        assertTrue(check.inspect(data).isEmpty());
    }

    @Test
    void flagsWithRampedConfidenceAboveTheScore() {
        check.next = OptionalDouble.of(4.0);
        Violation v = check.inspect(data).orElseThrow();
        assertEquals(4.0, v.value(), EPS);
        assertEquals(0.5, v.confidence(), EPS); // ramp(4, 2, 6)
    }

    @Test
    void confidenceSaturatesAtTheCeiling() {
        check.next = OptionalDouble.of(100.0);
        assertEquals(1.0, check.inspect(data).orElseThrow().confidence(), EPS);
    }

    @Test
    void neverFlagsWhenTheCriterionWithholdsAScore() {
        ValueCheck silent = new ValueCheck(samples -> OptionalDouble.empty(), 5, 2.0, 6.0);
        silent.next = OptionalDouble.of(99.0);
        assertTrue(silent.inspect(data).isEmpty());
    }

    @Test
    void constructorValidatesArguments() {
        assertThrows(IllegalArgumentException.class, () -> new ValueCheck(LATEST, 5, 2.0, 2.0));
        assertThrows(IllegalArgumentException.class, () -> new ValueCheck(LATEST, 0, 2.0, 6.0));
        assertThrows(NullPointerException.class, () -> new ValueCheck(null, 5, 2.0, 6.0));
    }
}
