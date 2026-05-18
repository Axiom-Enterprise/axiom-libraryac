package com.github.axiom.ac.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.api.Check;
import com.github.axiom.ac.api.EventBus;
import com.github.axiom.ac.api.PlayerData;
import com.github.axiom.ac.api.Violation;
import com.github.axiom.ac.api.event.CheckFaultEvent;
import com.github.axiom.ac.math.Vec3;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CheckRegistryTest {

    private record FakePlayerData(UUID uuid, Vec3 position, Vec3 velocity,
                                  float yaw, float pitch, boolean onGround)
            implements PlayerData {
    }

    private static PlayerData player() {
        return new FakePlayerData(UUID.randomUUID(), new Vec3(0, 0, 0),
                new Vec3(0, 0, 0), 0.0f, 0.0f, true);
    }

    /** Always flags. */
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

    /** Always throws. */
    private static Check throwing(String id) {
        return new Check() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Optional<Violation> inspect(PlayerData data) {
                throw new IllegalStateException("boom");
            }
        };
    }

    @Test
    void inspectCollectsViolationsFromEveryCheck() {
        CheckRegistry registry = new CheckRegistry(new EventBus());
        registry.register(flagging("speed"));
        registry.register(flagging("reach"));

        List<Violation> violations = registry.inspect(player());

        assertEquals(2, violations.size());
    }

    @Test
    void aThrowingCheckDoesNotBreakOthers() {
        CheckRegistry registry = new CheckRegistry(new EventBus());
        registry.register(throwing("bad"));
        registry.register(flagging("speed"));

        List<Violation> violations = registry.inspect(player());

        assertEquals(1, violations.size());
        assertEquals("speed", violations.get(0).checkId());
    }

    @Test
    void aCheckIsRemovedAfterReachingTheFaultThreshold() {
        EventBus bus = new EventBus();
        AtomicInteger faults = new AtomicInteger();
        bus.channel(CheckFaultEvent.class).subscribe(event -> faults.incrementAndGet());
        CheckRegistry registry = new CheckRegistry(bus, 3);
        registry.register(throwing("bad"));

        registry.inspect(player());
        registry.inspect(player());
        assertTrue(registry.isRegistered("bad"));
        assertEquals(0, faults.get());

        registry.inspect(player());

        assertFalse(registry.isRegistered("bad"));
        assertEquals(1, faults.get());
    }

    @Test
    void unregisterRemovesACheck() {
        CheckRegistry registry = new CheckRegistry(new EventBus());
        registry.register(flagging("speed"));
        registry.unregister("speed");
        assertFalse(registry.isRegistered("speed"));
        assertTrue(registry.inspect(player()).isEmpty());
    }
}
