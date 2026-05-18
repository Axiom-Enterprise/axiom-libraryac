package com.github.axiom.ac.api;

import java.util.List;
import java.util.UUID;

/**
 * Pluggable persistence SPI. The runtime ships in-memory and JSON
 * implementations; consumers may supply their own (for example
 * SQL-backed) implementation.
 */
public interface StorageProvider {

    /** Persists {@code violation} for the given player. */
    void saveViolation(UUID playerId, Violation violation);

    /**
     * Returns all stored violations for {@code playerId}, or an empty
     * list when none exist.
     */
    List<Violation> loadViolations(UUID playerId);
}
