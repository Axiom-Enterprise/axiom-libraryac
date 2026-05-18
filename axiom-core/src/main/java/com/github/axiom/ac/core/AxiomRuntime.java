package com.github.axiom.ac.core;

import com.github.axiom.ac.api.EventBus;
import com.github.axiom.ac.api.StorageProvider;
import com.github.axiom.ac.api.Violation;
import com.github.axiom.ac.api.event.FlagEvent;
import com.github.axiom.ac.api.event.PlayerJoinEvent;
import com.github.axiom.ac.api.event.PlayerQuitEvent;
import com.github.axiom.ac.packet.PlayerDataImpl;
import com.github.axiom.ac.packet.PlayerRegistry;
import com.github.axiom.ac.world.CollisionEngine;
import com.github.axiom.ac.world.WorldCache;
import java.util.Objects;
import java.util.UUID;

/**
 * Central runtime: wires together the event bus, the player
 * registry, the world model, the check registry and storage. Handles
 * player lifecycle and inspection passes.
 */
public final class AxiomRuntime {

    private final EventBus eventBus = new EventBus();
    private final PlayerRegistry players = new PlayerRegistry();
    private final WorldCache world = new WorldCache();
    private final CollisionEngine collision = new CollisionEngine(world);
    private final CheckRegistry checks = new CheckRegistry(eventBus);
    private final StorageProvider storage;

    public AxiomRuntime(StorageProvider storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    /** The event bus other plugins subscribe to. */
    public EventBus eventBus() {
        return eventBus;
    }

    /** The registry of tracked players. */
    public PlayerRegistry players() {
        return players;
    }

    /** The server-side world block cache. */
    public WorldCache world() {
        return world;
    }

    /** The collision engine over {@link #world()}. */
    public CollisionEngine collision() {
        return collision;
    }

    /** The registry of detection checks. */
    public CheckRegistry checks() {
        return checks;
    }

    /** The configured storage provider. */
    public StorageProvider storage() {
        return storage;
    }

    /** Starts tracking a player and publishes a {@link PlayerJoinEvent}. */
    public void handlePlayerJoin(UUID playerId) {
        players.register(playerId);
        eventBus.publish(new PlayerJoinEvent(playerId));
    }

    /** Stops tracking a player and publishes a {@link PlayerQuitEvent}. */
    public void handlePlayerQuit(UUID playerId) {
        players.unregister(playerId);
        eventBus.publish(new PlayerQuitEvent(playerId));
    }

    /**
     * Runs every registered check for the given player. Each
     * violation is published as a {@link FlagEvent}; a violation
     * whose event was not cancelled is persisted. Unknown players are
     * ignored.
     */
    public void inspect(UUID playerId) {
        PlayerDataImpl data = players.get(playerId).orElse(null);
        if (data == null) {
            return;
        }
        for (Violation violation : checks.inspect(data)) {
            FlagEvent event = new FlagEvent(playerId, violation);
            eventBus.publish(event);
            if (!event.isCancelled()) {
                storage.saveViolation(playerId, violation);
            }
        }
    }
}
