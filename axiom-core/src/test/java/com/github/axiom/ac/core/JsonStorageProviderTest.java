package com.github.axiom.ac.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.api.Violation;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonStorageProviderTest {

    @Test
    void unknownPlayerHasNoViolations(@TempDir Path dir) {
        JsonStorageProvider storage = new JsonStorageProvider(dir.resolve("v.json"));
        assertTrue(storage.loadViolations(UUID.randomUUID()).isEmpty());
    }

    @Test
    void savedViolationIsLoadedBackInSameInstance(@TempDir Path dir) {
        JsonStorageProvider storage = new JsonStorageProvider(dir.resolve("v.json"));
        UUID player = UUID.randomUUID();
        Violation violation = new Violation("speed", "too fast", 1.5, 0.8);

        storage.saveViolation(player, violation);

        assertEquals(java.util.List.of(violation), storage.loadViolations(player));
    }

    @Test
    void violationsPersistAcrossInstances(@TempDir Path dir) {
        Path file = dir.resolve("v.json");
        UUID player = UUID.randomUUID();
        Violation violation = new Violation("reach", "too far", 4.2, 0.95);

        new JsonStorageProvider(file).saveViolation(player, violation);
        JsonStorageProvider reopened = new JsonStorageProvider(file);

        assertEquals(java.util.List.of(violation), reopened.loadViolations(player));
    }
}
