package com.github.axiom.ac.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StorageProviderTest {

    /** Minimal in-memory test double proving the StorageProvider contract. */
    private static final class MemoryStorage implements StorageProvider {
        private final Map<UUID, List<Violation>> byPlayer = new HashMap<>();

        @Override
        public void saveViolation(UUID playerId, Violation violation) {
            byPlayer.computeIfAbsent(playerId, key -> new ArrayList<>()).add(violation);
        }

        @Override
        public List<Violation> loadViolations(UUID playerId) {
            return List.copyOf(byPlayer.getOrDefault(playerId, List.of()));
        }
    }

    @Test
    void savedViolationIsLoadedBack() {
        MemoryStorage storage = new MemoryStorage();
        UUID player = UUID.randomUUID();
        Violation violation = new Violation("speed", "too fast", 1.0, 0.9);

        storage.saveViolation(player, violation);

        assertEquals(List.of(violation), storage.loadViolations(player));
    }

    @Test
    void unknownPlayerHasNoViolations() {
        assertTrue(new MemoryStorage().loadViolations(UUID.randomUUID()).isEmpty());
    }
}
