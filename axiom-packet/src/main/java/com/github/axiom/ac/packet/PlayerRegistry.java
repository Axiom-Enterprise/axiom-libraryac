package com.github.axiom.ac.packet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of currently tracked players. Registration happens on the
 * server thread when a player joins; lookups happen on netty threads
 * during packet processing — hence the concurrent map.
 */
public final class PlayerRegistry {

    private final Map<UUID, PlayerDataImpl> players = new ConcurrentHashMap<>();

    /**
     * Creates and stores a fresh {@link PlayerDataImpl} for
     * {@code uuid}, returning it. An existing entry is replaced.
     */
    public PlayerDataImpl register(UUID uuid) {
        PlayerDataImpl data = new PlayerDataImpl(uuid);
        players.put(uuid, data);
        return data;
    }

    /** Stops tracking {@code uuid}. */
    public void unregister(UUID uuid) {
        players.remove(uuid);
    }

    /** The tracked data for {@code uuid}, if any. */
    public Optional<PlayerDataImpl> get(UUID uuid) {
        return Optional.ofNullable(players.get(uuid));
    }

    /** A snapshot of every tracked player. */
    public Collection<PlayerDataImpl> all() {
        return List.copyOf(players.values());
    }

    /** Number of currently tracked players. */
    public int size() {
        return players.size();
    }
}
