package com.github.axiom.ac.detect.prediction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import com.github.axiom.ac.predict.MovementInput;
import com.github.axiom.ac.predict.MovementPredictor;
import com.github.axiom.ac.predict.PlayerState;
import com.github.axiom.ac.predict.PredictionEngine;
import com.github.axiom.ac.world.CollisionEngine;
import com.github.axiom.ac.world.PhysicsSimulator;
import com.github.axiom.ac.world.WorldCache;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PredictionProbeTest {

    private final PredictionEngine engine =
            new PredictionEngine(new PhysicsSimulator(new CollisionEngine(new WorldCache())));
    private final MovementPredictor predictor = new MovementPredictor(engine);
    private final PredictionProbe probe = new PredictionProbe(predictor, 8);

    private final UUID id = UUID.randomUUID();
    private final PlayerState start =
            new PlayerState(new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, true);

    @Test
    void firstObservationHasNoBaseline() {
        assertEquals(0.0, probe.observe(id, start));
        assertTrue(probe.averageOffset(id).isEmpty());
    }

    @Test
    void aLegitimateMoveProbesNearZero() {
        probe.observe(id, start);
        Vec3 honest = engine.predict(start, new MovementInput(1, 0, false, true)).position();
        PlayerState next = new PlayerState(honest, new Vec3(0, 0, 0), 0.0f, true);

        double offset = probe.observe(id, next);

        assertTrue(offset < 1e-6);
        assertTrue(probe.averageOffset(id).orElseThrow() < 1e-6);
    }

    @Test
    void aTeleportProbesLarge() {
        probe.observe(id, start);
        PlayerState teleport = new PlayerState(new Vec3(50, 100, 0), new Vec3(0, 0, 0), 0.0f, true);
        assertTrue(probe.observe(id, teleport) > 10.0);
    }

    @Test
    void forgetClearsTheBaseline() {
        probe.observe(id, start);
        probe.observe(id, new PlayerState(new Vec3(0, 100, 1), new Vec3(0, 0, 0), 0.0f, true));
        probe.forget(id);
        // No baseline again, so the next observation reports zero.
        assertEquals(0.0, probe.observe(id, start));
        assertTrue(probe.averageOffset(id).isEmpty());
    }

    @Test
    void rejectsNulls() {
        assertThrows(NullPointerException.class, () -> new PredictionProbe(null, 8));
        assertThrows(NullPointerException.class, () -> probe.observe(null, start));
        assertThrows(NullPointerException.class, () -> probe.observe(id, null));
    }
}
