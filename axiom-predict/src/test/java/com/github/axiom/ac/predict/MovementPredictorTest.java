package com.github.axiom.ac.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import com.github.axiom.ac.world.CollisionEngine;
import com.github.axiom.ac.world.PhysicsSimulator;
import com.github.axiom.ac.world.WorldCache;
import org.junit.jupiter.api.Test;

class MovementPredictorTest {

    private static PredictionEngine engineInEmptyWorld() {
        PhysicsSimulator simulator = new PhysicsSimulator(new CollisionEngine(new WorldCache()));
        return new PredictionEngine(simulator);
    }

    private final PredictionEngine engine = engineInEmptyWorld();
    private final MovementPredictor predictor = new MovementPredictor(engine);

    @Test
    void rejectsNullEngine() {
        assertThrows(NullPointerException.class, () -> new MovementPredictor(null));
    }

    @Test
    void bestPredictionRejectsNullArguments() {
        PlayerState state = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, true);
        assertThrows(NullPointerException.class,
                () -> predictor.bestPrediction(null, new Vec3(0, 0, 0)));
        assertThrows(NullPointerException.class,
                () -> predictor.bestPrediction(state, null));
    }

    @Test
    void aLegitimateMoveHasANearZeroOffset() {
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, true);
        // Where the player legitimately lands after sprinting forward.
        Vec3 honest = engine.predict(start,
                new MovementInput(1, 0, false, true)).position();

        PredictionResult result = predictor.bestPrediction(start, honest);

        assertEquals(0.0, result.offset(), 1e-6);
    }

    @Test
    void anImpossibleMoveHasALargeOffset() {
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, true);
        // A teleport far beyond anything one tick of input can produce.
        Vec3 cheat = new Vec3(50, 100, 0);

        PredictionResult result = predictor.bestPrediction(start, cheat);

        assertTrue(result.offset() > 10.0);
    }

    @Test
    void resultCarriesTheBestInputAndPrediction() {
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, true);
        Vec3 honest = engine.predict(start,
                new MovementInput(1, 0, false, true)).position();

        PredictionResult result = predictor.bestPrediction(start, honest);

        // The chosen prediction is the one nearest the actual position.
        assertEquals(result.offset(),
                result.predicted().position().distance(honest), 1e-9);
    }
}
