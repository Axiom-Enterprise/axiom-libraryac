package com.github.axiom.ac.api.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class LifecycleEventsTest {

    private final UUID player = UUID.randomUUID();

    @Test
    void playerJoinEventCarriesPlayerId() {
        assertEquals(player, new PlayerJoinEvent(player).playerId());
    }

    @Test
    void playerQuitEventCarriesPlayerId() {
        assertEquals(player, new PlayerQuitEvent(player).playerId());
    }

    @Test
    void checkFaultEventCarriesCheckIdAndReason() {
        CheckFaultEvent event = new CheckFaultEvent("speed", "3 consecutive exceptions");
        assertEquals("speed", event.checkId());
        assertEquals("3 consecutive exceptions", event.reason());
    }

    @Test
    void eventsRejectNullArguments() {
        assertThrows(NullPointerException.class, () -> new PlayerJoinEvent(null));
        assertThrows(NullPointerException.class, () -> new PlayerQuitEvent(null));
        assertThrows(NullPointerException.class, () -> new CheckFaultEvent(null, "r"));
        assertThrows(NullPointerException.class, () -> new CheckFaultEvent("speed", null));
    }
}
