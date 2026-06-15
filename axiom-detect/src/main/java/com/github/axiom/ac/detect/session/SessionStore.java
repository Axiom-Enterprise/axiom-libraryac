package com.github.axiom.ac.detect.session;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Per-player state keyed by UUID. A {@link com.github.axiom.ac.api.Check}
 * is stateless by contract, so any memory a check needs between
 * inspections lives here instead. Each value is normally touched only
 * on its own player's packet thread; the map itself is concurrent so
 * the join/quit thread can add and evict entries safely.
 *
 * @param <S> the per-player state type
 */
public final class SessionStore<S> {

    private final Map<UUID, S> states = new ConcurrentHashMap<>();

    /**
     * Returns the state for {@code uuid}, creating it from
     * {@code factory} on first access.
     */
    public S getOrCreate(UUID uuid, Supplier<S> factory) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(factory, "factory");
        return states.computeIfAbsent(uuid, ignored -> factory.get());
    }

    /** The state for {@code uuid}, or empty if none has been created. */
    public Optional<S> get(UUID uuid) {
        return Optional.ofNullable(states.get(uuid));
    }

    /** Stores {@code state} for {@code uuid}, replacing any existing entry. */
    public void put(UUID uuid, S state) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(state, "state");
        states.put(uuid, state);
    }

    /** Drops the state for {@code uuid}; call this on player quit. */
    public void remove(UUID uuid) {
        states.remove(uuid);
    }

    /** Forgets every tracked player. */
    public void clear() {
        states.clear();
    }

    /** Number of players with stored state. */
    public int size() {
        return states.size();
    }
}
