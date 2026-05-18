package com.github.axiom.ac.predict;

/**
 * The outcome of a best-match prediction search.
 *
 * @param input     the candidate input whose prediction matched best
 * @param predicted the predicted player state for that input
 * @param offset    distance between the predicted position and the
 *                  client's actual position — the cheat signal
 */
public record PredictionResult(MovementInput input, PlayerState predicted,
                               double offset) {
}
