package com.github.axiom.ac.detect.prediction;

import com.github.axiom.ac.detect.session.SessionStore;
import com.github.axiom.ac.detect.statistical.SampleAccumulator;
import com.github.axiom.ac.predict.MovementPredictor;
import com.github.axiom.ac.predict.PlayerState;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Drives the stateless {@link MovementPredictor} from a live stream of
 * player states. It remembers each player's previous state, so every
 * new observation yields the prediction offset — the gap between where
 * the player landed and the closest legitimate single-tick move — and
 * keeps a rolling window of those offsets.
 *
 * <p>This is the bridge between {@code axiom-predict} and the check
 * frameworks: feed {@link #averageOffset} into a statistical or
 * heuristic check rather than reacting to one noisy tick. Call
 * {@link #forget} on player quit.
 */
public final class PredictionProbe {

    private final MovementPredictor predictor;
    private final SessionStore<PlayerState> previous = new SessionStore<>();
    private final SampleAccumulator offsets;

    /**
     * @param predictor  the best-match predictor to drive
     * @param windowSize number of recent offsets kept for averaging
     */
    public PredictionProbe(MovementPredictor predictor, int windowSize) {
        this.predictor = Objects.requireNonNull(predictor, "predictor");
        this.offsets = new SampleAccumulator(windowSize);
    }

    /**
     * Records {@code observed} for {@code uuid} and returns the
     * prediction offset against the previous observation. The first
     * observation for a player has no baseline and returns 0.
     */
    public double observe(UUID uuid, PlayerState observed) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(observed, "observed");
        PlayerState last = previous.get(uuid).orElse(null);
        previous.put(uuid, observed);
        if (last == null) {
            return 0.0;
        }
        double offset = predictor.bestPrediction(last, observed.position()).offset();
        offsets.record(uuid, offset);
        return offset;
    }

    /** Mean prediction offset over {@code uuid}'s window, or empty if none yet. */
    public OptionalDouble averageOffset(UUID uuid) {
        return offsets.mean(uuid);
    }

    /** Drops all tracking for {@code uuid}; call on player quit. */
    public void forget(UUID uuid) {
        previous.remove(uuid);
        offsets.forget(uuid);
    }
}
