package com.github.axiom.ac.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalLong;

/**
 * Tracks outstanding transactions for latency compensation. Sending
 * a transaction records the send time; confirming it yields the
 * round-trip time, which bounds when the client had received all
 * prior server state.
 *
 * <p>Alongside the raw round-trip time, the manager keeps an
 * exponentially weighted moving average — a {@linkplain
 * #smoothedRoundTrip() smoothed latency} that absorbs single-sample
 * spikes — and a {@linkplain #jitter() jitter} estimate, the EWMA of
 * the deviation between successive samples. Checks should compare
 * against the smoothed latency and widen their tolerance with the
 * jitter.
 *
 * <p>Not thread-safe — one instance per player, used only on that
 * player's netty thread.
 */
public final class TransactionManager {

    /** Weight given to the newest sample in the latency EWMAs. */
    public static final double SMOOTHING_FACTOR = 0.2;

    private final TransactionSink sink;
    private final Map<Integer, Long> pending = new HashMap<>();

    private int nextId = 1;
    private long lastRoundTripMillis = -1L;
    private double smoothedRoundTripMillis = -1.0;
    private double jitterMillis = -1.0;

    public TransactionManager(TransactionSink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * Issues a transaction: allocates a unique id, records
     * {@code nowMillis} as its send time, asks the sink to send it,
     * and returns the id.
     */
    public int sendTransaction(long nowMillis) {
        int id = nextId++;
        pending.put(id, nowMillis);
        sink.send(id);
        return id;
    }

    /**
     * Confirms the transaction {@code id} as echoed back at
     * {@code nowMillis}. Returns the round-trip time in milliseconds,
     * or empty when the id is unknown (never sent, or already
     * confirmed).
     */
    public OptionalLong confirm(int id, long nowMillis) {
        Long sentAt = pending.remove(id);
        if (sentAt == null) {
            return OptionalLong.empty();
        }
        long roundTrip = nowMillis - sentAt;
        lastRoundTripMillis = roundTrip;
        updateSmoothing(roundTrip);
        return OptionalLong.of(roundTrip);
    }

    /**
     * Folds a fresh round-trip sample into the smoothed-latency and
     * jitter EWMAs. The first sample seeds the average directly and
     * leaves the jitter at zero.
     */
    private void updateSmoothing(long roundTrip) {
        if (smoothedRoundTripMillis < 0.0) {
            smoothedRoundTripMillis = roundTrip;
            jitterMillis = 0.0;
            return;
        }
        double deviation = Math.abs(roundTrip - smoothedRoundTripMillis);
        jitterMillis = (1.0 - SMOOTHING_FACTOR) * jitterMillis
                + SMOOTHING_FACTOR * deviation;
        smoothedRoundTripMillis = (1.0 - SMOOTHING_FACTOR) * smoothedRoundTripMillis
                + SMOOTHING_FACTOR * roundTrip;
    }

    /** True while transaction {@code id} is sent but not yet confirmed. */
    public boolean isPending(int id) {
        return pending.containsKey(id);
    }

    /** Number of sent-but-unconfirmed transactions. */
    public int pendingCount() {
        return pending.size();
    }

    /** Round-trip time of the most recent confirmation, if any. */
    public OptionalLong lastRoundTrip() {
        return lastRoundTripMillis < 0L
                ? OptionalLong.empty()
                : OptionalLong.of(lastRoundTripMillis);
    }

    /**
     * Exponentially weighted moving average of the round-trip time,
     * in milliseconds — a spike-resistant latency estimate. Empty
     * until the first confirmation.
     */
    public OptionalDouble smoothedRoundTrip() {
        return smoothedRoundTripMillis < 0.0
                ? OptionalDouble.empty()
                : OptionalDouble.of(smoothedRoundTripMillis);
    }

    /**
     * Estimated latency jitter, in milliseconds — the EWMA of the
     * deviation between successive round-trip samples. Empty until
     * the first confirmation; zero after a single sample.
     */
    public OptionalDouble jitter() {
        return jitterMillis < 0.0
                ? OptionalDouble.empty()
                : OptionalDouble.of(jitterMillis);
    }
}
