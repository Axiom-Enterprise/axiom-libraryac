package com.github.axiom.ac.detect.heuristic;

import com.github.axiom.ac.api.Check;
import com.github.axiom.ac.api.PlayerData;
import com.github.axiom.ac.api.Violation;
import com.github.axiom.ac.detect.session.SessionStore;
import com.github.axiom.ac.detect.signal.Confidence;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Base class for threshold-with-decay heuristic checks — the standard
 * anticheat pattern where a single suspicious tick is not enough, but
 * a sustained pattern is. Subclasses implement {@link #evaluate} to
 * judge one snapshot; this class accumulates the resulting weights
 * into a per-player {@link ViolationLevel}, decays it on clean ticks,
 * and emits a {@link Violation} once the level crosses the flag
 * threshold.
 *
 * <p>The per-player level is held in a {@link SessionStore}, keeping
 * the subclass's judgement logic stateless. Call {@link #forget} on
 * player quit.
 */
public abstract class AbstractHeuristicCheck implements Check {

    private final String id;
    private final double flagThreshold;
    private final double saturationLevel;
    private final double decayPerPass;
    private final SessionStore<ViolationLevel> levels = new SessionStore<>();

    /**
     * @param id              stable, unique check id
     * @param flagThreshold   violation level at which the check starts flagging
     * @param saturationLevel level at which confidence reaches 1; must exceed the threshold
     * @param decayPerPass    amount the level decays on each clean tick
     */
    protected AbstractHeuristicCheck(String id, double flagThreshold,
                                     double saturationLevel, double decayPerPass) {
        this.id = Objects.requireNonNull(id, "id");
        if (flagThreshold <= 0.0) {
            throw new IllegalArgumentException("flagThreshold must be > 0");
        }
        if (saturationLevel <= flagThreshold) {
            throw new IllegalArgumentException("saturationLevel must exceed flagThreshold");
        }
        if (decayPerPass < 0.0) {
            throw new IllegalArgumentException("decayPerPass must be >= 0");
        }
        this.flagThreshold = flagThreshold;
        this.saturationLevel = saturationLevel;
        this.decayPerPass = decayPerPass;
    }

    @Override
    public final String id() {
        return id;
    }

    @Override
    public final Optional<Violation> inspect(PlayerData data) {
        HeuristicSignal signal = evaluate(data);
        ViolationLevel vl = levels.getOrCreate(data.uuid(), ViolationLevel::new);
        if (signal.failed()) {
            vl.add(signal.weight());
        } else {
            vl.decay(decayPerPass);
        }

        double level = vl.level();
        if (level < flagThreshold) {
            return Optional.empty();
        }
        double confidence = Confidence.ramp(level, flagThreshold, saturationLevel);
        String detail = signal.failed() ? signal.detail() : "sustained violation level";
        return Optional.of(new Violation(id, detail, level, confidence));
    }

    /**
     * Judges a single snapshot. Return {@link HeuristicSignal#fail} with
     * a weight when the tick is suspicious, or {@link HeuristicSignal#pass}
     * when it is clean. Must not call the Bukkit API — it runs on a
     * packet thread.
     */
    protected abstract HeuristicSignal evaluate(PlayerData data);

    /** Drops the accumulated level for {@code uuid}; call on player quit. */
    public final void forget(UUID uuid) {
        levels.remove(uuid);
    }

    /** Clears every tracked player's level. */
    public final void reset() {
        levels.clear();
    }
}
