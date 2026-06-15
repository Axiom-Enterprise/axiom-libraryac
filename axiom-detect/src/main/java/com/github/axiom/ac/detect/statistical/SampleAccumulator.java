package com.github.axiom.ac.detect.statistical;

import com.github.axiom.ac.detect.session.SessionStore;
import com.github.axiom.ac.math.SlidingWindow;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Keeps a fixed-size {@link SlidingWindow} of recent samples per
 * player, so a {@link StatisticalCriterion} always sees the player's
 * own rolling history. Each window is thread-confined to its player's
 * packet thread; the backing {@link SessionStore} handles the
 * cross-thread bookkeeping.
 */
public final class SampleAccumulator {

    private final int windowSize;
    private final SessionStore<SlidingWindow> windows = new SessionStore<>();

    public SampleAccumulator(int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be positive");
        }
        this.windowSize = windowSize;
    }

    /**
     * Appends {@code value} to {@code uuid}'s window and returns the
     * retained samples, oldest first.
     */
    public double[] record(UUID uuid, double value) {
        SlidingWindow window = windows.getOrCreate(uuid, () -> new SlidingWindow(windowSize));
        window.add(value);
        return window.toArray();
    }

    /** Mean of {@code uuid}'s retained samples, or empty if none yet. */
    public OptionalDouble mean(UUID uuid) {
        return windows.get(uuid)
                .filter(window -> window.size() > 0)
                .map(window -> OptionalDouble.of(window.mean()))
                .orElseGet(OptionalDouble::empty);
    }

    /** Drops {@code uuid}'s window; call on player quit. */
    public void forget(UUID uuid) {
        windows.remove(uuid);
    }

    /** Forgets every tracked player. */
    public void reset() {
        windows.clear();
    }
}
