package com.github.axiom.ac.api.event;

import java.util.Objects;
import java.util.UUID;

/**
 * Fired when Axiom starts tracking a player.
 *
 * @param playerId id of the player now being tracked
 */
public record PlayerJoinEvent(UUID playerId) {

    public PlayerJoinEvent {
        Objects.requireNonNull(playerId, "playerId");
    }
}
