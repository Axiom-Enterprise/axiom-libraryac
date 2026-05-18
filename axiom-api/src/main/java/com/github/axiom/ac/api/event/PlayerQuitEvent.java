package com.github.axiom.ac.api.event;

import java.util.Objects;
import java.util.UUID;

/**
 * Fired when Axiom stops tracking a player.
 *
 * @param playerId id of the player no longer being tracked
 */
public record PlayerQuitEvent(UUID playerId) {

    public PlayerQuitEvent {
        Objects.requireNonNull(playerId, "playerId");
    }
}
