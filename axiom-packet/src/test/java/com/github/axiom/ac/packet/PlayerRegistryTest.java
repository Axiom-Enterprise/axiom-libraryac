package com.github.axiom.ac.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerRegistryTest {

    @Test
    void registerCreatesAndStoresPlayerData() {
        PlayerRegistry registry = new PlayerRegistry();
        UUID id = UUID.randomUUID();

        PlayerDataImpl data = registry.register(id);

        assertEquals(id, data.uuid());
        assertSame(data, registry.get(id).orElseThrow());
    }

    @Test
    void unknownPlayerReturnsEmpty() {
        assertTrue(new PlayerRegistry().get(UUID.randomUUID()).isEmpty());
    }

    @Test
    void unregisterRemovesPlayerData() {
        PlayerRegistry registry = new PlayerRegistry();
        UUID id = UUID.randomUUID();
        registry.register(id);

        registry.unregister(id);

        assertTrue(registry.get(id).isEmpty());
    }

    @Test
    void sizeReflectsRegisteredPlayers() {
        PlayerRegistry registry = new PlayerRegistry();
        assertEquals(0, registry.size());
        registry.register(UUID.randomUUID());
        registry.register(UUID.randomUUID());
        assertEquals(2, registry.size());
    }

    @Test
    void allReturnsEveryRegisteredPlayer() {
        PlayerRegistry registry = new PlayerRegistry();
        registry.register(UUID.randomUUID());
        registry.register(UUID.randomUUID());
        assertEquals(2, registry.all().size());
    }
}
