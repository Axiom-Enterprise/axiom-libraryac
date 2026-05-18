package com.github.axiom.ac.api;

import java.util.Optional;

/**
 * Detection check SPI. Consumers implement this to add detection
 * logic. A check is stateless — any per-player state belongs in the
 * {@link PlayerData} implementation or in storage. Implementations
 * must be safe to call from the packet-processing thread and must
 * not call the Bukkit API directly.
 */
public interface Check {

    /** Stable, unique id of this check (for example {@code "speed"}). */
    String id();

    /**
     * Inspects a player snapshot. Returns a {@link Violation} when
     * the check detects something, or empty otherwise.
     */
    Optional<Violation> inspect(PlayerData data);
}
