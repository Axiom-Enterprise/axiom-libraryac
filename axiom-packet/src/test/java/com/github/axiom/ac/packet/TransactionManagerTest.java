package com.github.axiom.ac.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
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
}
