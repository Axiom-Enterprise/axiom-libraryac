package com.github.axiom.ac.detect.heuristic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.api.PlayerData;
import com.github.axiom.ac.api.Violation;
import com.github.axiom.ac.detect.FakePlayerData;
import com.github.axiom.ac.math.Vec3;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AbstractHeuristicCheckTest {

    private static final double EPS = 1e-9;

    /** Test check whose evaluation result is set per call. */
    private static final class ProgrammableCheck extends AbstractHeuristicCheck {
        private HeuristicSignal next = HeuristicSignal.pass();

        ProgrammableCheck(double flag, double saturation, double decay) {
            super("test", flag, saturation, decay);
        }

        @Override
        protected HeuristicSignal evaluate(PlayerData data) {
            return next;
        }
    }

    private final UUID id = UUID.randomUUID();
    private final PlayerData data = FakePlayerData.at(id, new Vec3(0, 0, 0));
    private final ProgrammableCheck check = new ProgrammableCheck(10.0, 30.0, 4.0);

    @Test
    void exposesItsId() {
        assertEquals("test", check.id());
    }

    @Test
    void staysQuietBelowThreshold() {
        check.next = HeuristicSignal.fail(3.0, "blip");
        assertTrue(check.inspect(data).isEmpty()); // 3
        assertTrue(check.inspect(data).isEmpty()); // 6
        assertTrue(check.inspect(data).isEmpty()); // 9
    }

    @Test
    void flagsWithRampedConfidenceOnceOverThreshold() {
        check.next = HeuristicSignal.fail(20.0, "boom");
        Optional<Violation> flagged = check.inspect(data);
        assertTrue(flagged.isPresent());
        Violation v = flagged.orElseThrow();
        assertEquals(20.0, v.value(), EPS);
        assertEquals(0.5, v.confidence(), EPS); // ramp(20, 10, 30)
        assertEquals("boom", v.description());
    }

    @Test
    void cleanTicksDecayTheLevelUntilItStopsFlagging() {
        check.next = HeuristicSignal.fail(20.0, "boom");
        check.inspect(data); // level 20, flags

        check.next = HeuristicSignal.pass();
        assertTrue(check.inspect(data).isPresent()); // 16, still over 10
        assertTrue(check.inspect(data).isPresent()); // 12
        assertTrue(check.inspect(data).isEmpty());    // 8, under threshold
    }

    @Test
    void forgetResetsTheLevel() {
        check.next = HeuristicSignal.fail(20.0, "boom");
        check.inspect(data);
        check.forget(id);

        check.next = HeuristicSignal.fail(3.0, "blip");
        assertTrue(check.inspect(data).isEmpty()); // back to 3
    }

    @Test
    void constructorValidatesBounds() {
        assertThrows(IllegalArgumentException.class, () -> new ProgrammableCheck(0.0, 30.0, 4.0));
        assertThrows(IllegalArgumentException.class, () -> new ProgrammableCheck(10.0, 10.0, 4.0));
        assertThrows(IllegalArgumentException.class, () -> new ProgrammableCheck(10.0, 30.0, -1.0));
        assertThrows(NullPointerException.class,
                () -> new AbstractHeuristicCheck(null, 10.0, 30.0, 4.0) {
                    @Override
                    protected HeuristicSignal evaluate(PlayerData data) {
                        return HeuristicSignal.pass();
                    }
                });
    }
}
