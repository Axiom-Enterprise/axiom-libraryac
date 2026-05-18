package com.github.axiom.ac.api.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.api.Violation;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FlagEventTest {

    private final UUID player = UUID.randomUUID();
    private final Violation violation = new Violation("speed", "too fast", 1.0, 0.9);

    @Test
    void exposesPlayerAndViolation() {
        FlagEvent event = new FlagEvent(player, violation);
        assertEquals(player, event.playerId());
        assertEquals(violation, event.violation());
    }

    @Test
    void isNotCancelledByDefault() {
        assertFalse(new FlagEvent(player, violation).isCancelled());
    }

    @Test
    void canBeCancelled() {
        FlagEvent event = new FlagEvent(player, violation);
        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new FlagEvent(null, violation));
        assertThrows(NullPointerException.class, () -> new FlagEvent(player, null));
    }
}
