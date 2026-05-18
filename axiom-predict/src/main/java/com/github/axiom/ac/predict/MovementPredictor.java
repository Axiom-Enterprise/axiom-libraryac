package com.github.axiom.ac.predict;

import com.github.axiom.ac.math.Vec3;
import java.util.Objects;

/**
 * Searches the whole {@link InputSpace} for the input whose predicted
 * outcome lands closest to where the client actually claims to be.
 * The resulting {@link PredictionResult#offset()} is the movement
 * cheat signal: near zero for legitimate play, large when no
 * legitimate input explains the move.
 */
public final class MovementPredictor {

    private final PredictionEngine engine;

    public MovementPredictor(PredictionEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    /**
     * Returns the best-matching prediction for a player moving from
     * {@code previous} to {@code actualPosition}.
     */
    public PredictionResult bestPrediction(PlayerState previous, Vec3 actualPosition) {
        PredictionResult best = null;
        for (MovementInput input : InputSpace.all()) {
            PlayerState predicted = engine.predict(previous, input);
            double offset = predicted.position().distance(actualPosition);
            if (best == null || offset < best.offset()) {
                best = new PredictionResult(input, predicted, offset);
            }
        }
        return best;
    }
}
