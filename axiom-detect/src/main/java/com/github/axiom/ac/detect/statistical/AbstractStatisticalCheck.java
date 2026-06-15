package com.github.axiom.ac.detect.statistical;

import com.github.axiom.ac.api.Check;
import com.github.axiom.ac.api.PlayerData;
import com.github.axiom.ac.api.Violation;
import com.github.axiom.ac.detect.signal.Confidence;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Base class for checks that flag a statistical anomaly in a stream of
 * per-player samples. A subclass implements {@link #sample} to pull
 * one number out of a snapshot (a speed, an interval, an angle delta);
 * this class feeds it into a rolling window, scores the window with a
 * {@link StatisticalCriterion}, and emits a {@link Violation} once the
 * score crosses the flag threshold.
 *
 * <p>Per-player windows live in a {@link SampleAccumulator}, so the
 * subclass stays stateless. Call {@link #forget} on player quit.
 */
public abstract class AbstractStatisticalCheck implements Check {

    private final String id;
    private final StatisticalCriterion criterion;
    private final double flagScore;
    private final double saturationScore;
    private final SampleAccumulator samples;

    /**
     * @param id              stable, unique check id
     * @param criterion       the scoring criterion
     * @param windowSize      number of recent samples scored together
     * @param flagScore       score at or above which the check flags
     * @param saturationScore score at which confidence reaches 1; must exceed the flag score
     */
    protected AbstractStatisticalCheck(String id, StatisticalCriterion criterion,
                                       int windowSize, double flagScore, double saturationScore) {
        this.id = Objects.requireNonNull(id, "id");
        this.criterion = Objects.requireNonNull(criterion, "criterion");
        if (saturationScore <= flagScore) {
            throw new IllegalArgumentException("saturationScore must exceed flagScore");
        }
        this.flagScore = flagScore;
        this.saturationScore = saturationScore;
        this.samples = new SampleAccumulator(windowSize);
    }

    @Override
    public final String id() {
        return id;
    }

    @Override
    public final Optional<Violation> inspect(PlayerData data) {
        OptionalDouble sample = sample(data);
        if (sample.isEmpty()) {
            return Optional.empty();
        }
        double[] window = samples.record(data.uuid(), sample.getAsDouble());
        OptionalDouble score = criterion.score(window);
        if (score.isEmpty() || score.getAsDouble() < flagScore) {
            return Optional.empty();
        }
        double value = score.getAsDouble();
        double confidence = Confidence.ramp(value, flagScore, saturationScore);
        return Optional.of(new Violation(id, "statistical score " + value, value, confidence));
    }

    /**
     * Extracts the sample to track from a snapshot, or empty to skip
     * this tick without recording anything (for example when the
     * sample is undefined). Must not call the Bukkit API.
     */
    protected abstract OptionalDouble sample(PlayerData data);

    /** Drops {@code uuid}'s sample window; call on player quit. */
    public final void forget(UUID uuid) {
        samples.forget(uuid);
    }

    /** Clears every tracked player's window. */
    public final void reset() {
        samples.reset();
    }
}
