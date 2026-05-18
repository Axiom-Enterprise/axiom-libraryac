package com.github.axiom.ac.packet;

/**
 * Sends a transaction (ping) packet carrying the given id to the
 * client. The PacketEvents-backed implementation lives in the
 * packet pipeline; tests supply a fake.
 */
@FunctionalInterface
public interface TransactionSink {

    /** Sends a transaction packet carrying {@code transactionId}. */
    void send(int transactionId);
}
