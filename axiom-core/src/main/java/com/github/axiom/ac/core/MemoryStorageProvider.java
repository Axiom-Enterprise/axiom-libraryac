package com.github.axiom.ac.core;

import com.github.axiom.ac.api.StorageProvider;
import com.github.axiom.ac.api.Violation;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory {@link StorageProvider}. Violations live only for the
 * lifetime of the process — nothing is persisted. This is the
 * default provider.
 */
public final class MemoryStorageProvider implements StorageProvider {

    private final Map<UUID, List<Violation>> byPlayer = new ConcurrentHashMap<>();

    @Override
    public void saveViolation(UUID playerId, Violation violation) {
        byPlayer.computeIfAbsent(playerId, key -> new CopyOnWriteArrayList<>())
                .add(violation);
    }

    @Override
    public List<Violation> loadViolations(UUID playerId) {
        return List.copyOf(byPlayer.getOrDefault(playerId, List.of()));
    }
}
