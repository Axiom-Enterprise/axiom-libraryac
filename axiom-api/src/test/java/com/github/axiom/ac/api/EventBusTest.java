package com.github.axiom.ac.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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

        assertEquals(null, received.get());
    }
}
