package com.github.axiom.ac.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Typed publish/subscribe channel for a single event type. Handlers
 * are invoked synchronously on the publishing thread, in subscription
 * order.
 *
 * <p>Safe to publish while handlers are being added or removed.
 *
 * @param <T> event type carried by this channel
 */
public final class EventChannel<T> {

    private final List<Consumer<T>> handlers = new CopyOnWriteArrayList<>();

    /**
     * Registers {@code handler}. The returned {@link Subscription}
     * removes it when closed.
     */
    public Subscription subscribe(Consumer<T> handler) {
        handlers.add(handler);
        return () -> handlers.remove(handler);
    }

    /** Delivers {@code event} to every current subscriber. */
    public void publish(T event) {
        for (Consumer<T> handler : handlers) {
            handler.accept(event);
        }
    }

    /** Number of currently registered subscribers. */
    public int subscriberCount() {
        return handlers.size();
    }
}
