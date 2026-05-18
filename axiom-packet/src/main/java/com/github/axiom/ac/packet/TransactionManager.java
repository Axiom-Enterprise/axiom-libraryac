package com.github.axiom.ac.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Tracks outstanding transactions for latency compensation. Sending
 * a transaction records the send time; confirming it yields the
 * round-trip time, which bounds when the client had received all
 * prior server state.
 *
 * <p>Not thread-safe — one instance per player, used only on that
 * player's netty thread.
 */
public final class TransactionManager {

    private final TransactionSink sink;
    private final Map<Integer, Long> pending = new HashMap<>();

    private int nextId = 1;
    private long lastRoundTripMillis = -1L;

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
        return OptionalLong.of(roundTrip);
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
}
