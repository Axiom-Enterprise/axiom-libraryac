package com.github.axiom.ac.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.api.Check;
import com.github.axiom.ac.api.PlayerData;
import com.github.axiom.ac.api.Violation;
import com.github.axiom.ac.api.event.FlagEvent;
import com.github.axiom.ac.api.event.PlayerJoinEvent;
import com.github.axiom.ac.api.event.PlayerQuitEvent;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AxiomRuntimeTest {

    private static Check flagging(String id) {
        return new Check() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Optional<Violation> inspect(PlayerData data) {
                return Optional.of(new Violation(id, "flagged", 1.0, 1.0));
            }
        };
    }

    @Test
    void playerJoinRegistersAndPublishes() {
        AxiomRuntime runtime = new AxiomRuntime(new MemoryStorageProvider());
        AtomicInteger joins = new AtomicInteger();
        runtime.eventBus().channel(PlayerJoinEvent.class).subscribe(e -> joins.incrementAndGet());
        UUID player = UUID.randomUUID();

        runtime.handlePlayerJoin(player);

        assertEquals(1, joins.get());
        assertTrue(runtime.players().get(player).isPresent());
    }

    @Test
    void playerQuitUnregistersAndPublishes() {
        AxiomRuntime runtime = new AxiomRuntime(new MemoryStorageProvider());
        AtomicInteger quits = new AtomicInteger();
        runtime.eventBus().channel(PlayerQuitEvent.class).subscribe(e -> quits.incrementAndGet());
        UUID player = UUID.randomUUID();
        runtime.handlePlayerJoin(player);

        runtime.handlePlayerQuit(player);

        assertEquals(1, quits.get());
        assertFalse(runtime.players().get(player).isPresent());
    }

    @Test
    void inspectPublishesFlagEventAndPersistsViolation() {
        MemoryStorageProvider storage = new MemoryStorageProvider();
        AxiomRuntime runtime = new AxiomRuntime(storage);
        runtime.checks().register(flagging("speed"));
        AtomicInteger flags = new AtomicInteger();
        runtime.eventBus().channel(FlagEvent.class).subscribe(e -> flags.incrementAndGet());
        UUID player = UUID.randomUUID();
        runtime.handlePlayerJoin(player);

        runtime.inspect(player);

        assertEquals(1, flags.get());
        assertEquals(1, storage.loadViolations(player).size());
    }

    @Test
    void cancelledFlagEventIsNotPersisted() {
        MemoryStorageProvider storage = new MemoryStorageProvider();
        AxiomRuntime runtime = new AxiomRuntime(storage);
        runtime.checks().register(flagging("speed"));
        runtime.eventBus().channel(FlagEvent.class).subscribe(e -> e.setCancelled(true));
        UUID player = UUID.randomUUID();
        runtime.handlePlayerJoin(player);

        runtime.inspect(player);

        assertTrue(storage.loadViolations(player).isEmpty());
    }

    @Test
    void inspectingAnUnknownPlayerDoesNothing() {
        AxiomRuntime runtime = new AxiomRuntime(new MemoryStorageProvider());
        runtime.checks().register(flagging("speed"));
        // No join — player is unknown.
        runtime.inspect(UUID.randomUUID());
        // No exception, nothing to assert beyond reaching here.
        assertEquals(0, runtime.players().size());
    }
}
