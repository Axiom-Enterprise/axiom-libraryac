package com.github.axiom.ac.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.api.Violation;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MemoryStorageProviderTest {

    private final MemoryStorageProvider storage = new MemoryStorageProvider();

    @Test
    void unknownPlayerHasNoViolations() {
        assertTrue(storage.loadViolations(UUID.randomUUID()).isEmpty());
    }

    @Test
    void savedViolationsAreLoadedBackInOrder() {
        UUID player = UUID.randomUUID();
        Violation first = new Violation("speed", "a", 1.0, 0.5);
        Violation second = new Violation("reach", "b", 2.0, 0.9);

        storage.saveViolation(player, first);
        storage.saveViolation(player, second);

        assertEquals(List.of(first, second), storage.loadViolations(player));
    }

    @Test
    void violationsAreKeyedPerPlayer() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        storage.saveViolation(a, new Violation("speed", "a", 1.0, 0.5));

        assertEquals(1, storage.loadViolations(a).size());
        assertTrue(storage.loadViolations(b).isEmpty());
    }
}
