package com.github.axiom.ac.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class TransactionManagerTest {

    /** Fake sink recording every transaction id it was asked to send. */
    private static final class RecordingSink implements TransactionSink {
        final List<Integer> sent = new ArrayList<>();

        @Override
        public void send(int transactionId) {
            sent.add(transactionId);
        }
    }

    @Test
    void rejectsNullSink() {
        assertThrows(NullPointerException.class, () -> new TransactionManager(null));
    }

    @Test
    void sendTransactionForwardsToSinkAndReturnsId() {
        RecordingSink sink = new RecordingSink();
        TransactionManager manager = new TransactionManager(sink);

        int id = manager.sendTransaction(1_000L);

        assertEquals(List.of(id), sink.sent);
        assertTrue(manager.isPending(id));
        assertEquals(1, manager.pendingCount());
    }

    @Test
    void transactionIdsAreUnique() {
        TransactionManager manager = new TransactionManager(new RecordingSink());
        int first = manager.sendTransaction(0L);
        int second = manager.sendTransaction(0L);
        assertFalse(first == second);
    }

    @Test
    void confirmReturnsRoundTripAndClearsPending() {
        TransactionManager manager = new TransactionManager(new RecordingSink());
        int id = manager.sendTransaction(1_000L);

        OptionalLong rtt = manager.confirm(id, 1_050L);

        assertEquals(OptionalLong.of(50L), rtt);
        assertFalse(manager.isPending(id));
        assertEquals(0, manager.pendingCount());
    }

    @Test
    void confirmOfUnknownIdReturnsEmpty() {
        TransactionManager manager = new TransactionManager(new RecordingSink());
        assertEquals(OptionalLong.empty(), manager.confirm(999, 1_000L));
    }

    @Test
    void confirmTwiceReturnsEmptyTheSecondTime() {
        TransactionManager manager = new TransactionManager(new RecordingSink());
        int id = manager.sendTransaction(1_000L);
        manager.confirm(id, 1_010L);
        assertEquals(OptionalLong.empty(), manager.confirm(id, 1_020L));
    }

    @Test
    void lastRoundTripTracksMostRecentConfirm() {
        TransactionManager manager = new TransactionManager(new RecordingSink());
        assertEquals(OptionalLong.empty(), manager.lastRoundTrip());

        int id = manager.sendTransaction(1_000L);
        manager.confirm(id, 1_030L);

        assertEquals(OptionalLong.of(30L), manager.lastRoundTrip());
    }

    /** Confirms one transaction sent at t=0 with the given round-trip. */
    private static void confirmRoundTrip(TransactionManager manager, long roundTrip) {
        int id = manager.sendTransaction(0L);
        manager.confirm(id, roundTrip);
    }

    @Test
    void smoothedRoundTripAndJitterAreEmptyBeforeAnyConfirm() {
        TransactionManager manager = new TransactionManager(new RecordingSink());
        assertEquals(OptionalDouble.empty(), manager.smoothedRoundTrip());
        assertEquals(OptionalDouble.empty(), manager.jitter());
    }

    @Test
    void firstSampleSeedsSmoothedRoundTripAndZeroJitter() {
        TransactionManager manager = new TransactionManager(new RecordingSink());
        confirmRoundTrip(manager, 50L);
        assertEquals(OptionalDouble.of(50.0), manager.smoothedRoundTrip());
        assertEquals(OptionalDouble.of(0.0), manager.jitter());
    }

    @Test
    void smoothedRoundTripIsAnExponentialMovingAverage() {
        TransactionManager manager = new TransactionManager(new RecordingSink());
        confirmRoundTrip(manager, 100L);
        confirmRoundTrip(manager, 200L);
        // 0.8 * 100 + 0.2 * 200 = 120.
        assertEquals(120.0, manager.smoothedRoundTrip().orElseThrow(), 1e-9);
    }

    @Test
    void jitterRisesWithVariableLatency() {
        TransactionManager manager = new TransactionManager(new RecordingSink());
        confirmRoundTrip(manager, 100L);
        confirmRoundTrip(manager, 200L);
        // |200 - 100| weighted in at 0.2: 0.8 * 0 + 0.2 * 100 = 20.
        assertEquals(20.0, manager.jitter().orElseThrow(), 1e-9);
    }

    @Test
    void jitterStaysLowForStableLatency() {
        TransactionManager manager = new TransactionManager(new RecordingSink());
        for (int i = 0; i < 10; i++) {
            confirmRoundTrip(manager, 60L);
        }
        assertEquals(0.0, manager.jitter().orElseThrow(), 1e-9);
        assertEquals(60.0, manager.smoothedRoundTrip().orElseThrow(), 1e-9);
    }
}
