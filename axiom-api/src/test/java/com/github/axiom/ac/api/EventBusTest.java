package com.github.axiom.ac.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class EventBusTest {

    @Test
    void channelForSameTypeIsReused() {
        EventBus bus = new EventBus();
        EventChannel<String> first = bus.channel(String.class);
        EventChannel<String> second = bus.channel(String.class);
        assertSame(first, second);
    }

    @Test
    void publishRoutesToChannelOfEventType() {
        EventBus bus = new EventBus();
        AtomicReference<String> received = new AtomicReference<>();
        bus.channel(String.class).subscribe(received::set);

        bus.publish("hello");

        assertEquals("hello", received.get());
    }

    @Test
    void publishDoesNotReachOtherTypes() {
        EventBus bus = new EventBus();
        AtomicReference<Integer> received = new AtomicReference<>();
        bus.channel(Integer.class).subscribe(received::set);

        bus.publish("hello");

        assertNull(received.get());
    }

    @Test
    void publishWithNoChannelDropsEventSilently() {
        EventBus bus = new EventBus();
        assertDoesNotThrow(() -> bus.publish("nobody is listening"));
    }

    @Test
    void publishRejectsNullEvent() {
        EventBus bus = new EventBus();
        assertThrows(NullPointerException.class, () -> bus.publish(null));
    }
}
