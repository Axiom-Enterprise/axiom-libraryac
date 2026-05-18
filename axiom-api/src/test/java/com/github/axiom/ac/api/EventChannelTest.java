package com.github.axiom.ac.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class EventChannelTest {

    @Test
    void publishDeliversToSubscriber() {
        EventChannel<String> channel = new EventChannel<>();
        AtomicInteger calls = new AtomicInteger();
        channel.subscribe(event -> calls.incrementAndGet());

        channel.publish("hello");

        assertEquals(1, calls.get());
    }

    @Test
    void publishDeliversToEverySubscriber() {
        EventChannel<String> channel = new EventChannel<>();
        AtomicInteger calls = new AtomicInteger();
        channel.subscribe(event -> calls.incrementAndGet());
        channel.subscribe(event -> calls.incrementAndGet());

        channel.publish("hello");

        assertEquals(2, calls.get());
    }

    @Test
    void unsubscribeStopsDelivery() {
        EventChannel<String> channel = new EventChannel<>();
        AtomicInteger calls = new AtomicInteger();
        Subscription subscription = channel.subscribe(event -> calls.incrementAndGet());

        subscription.unsubscribe();
        channel.publish("hello");

        assertEquals(0, calls.get());
    }

    @Test
    void subscriberCountReflectsSubscriptions() {
        EventChannel<String> channel = new EventChannel<>();
        assertEquals(0, channel.subscriberCount());
        Subscription s = channel.subscribe(event -> { });
        assertEquals(1, channel.subscriberCount());
        s.unsubscribe();
        assertEquals(0, channel.subscriberCount());
    }
}
