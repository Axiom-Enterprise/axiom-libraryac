package com.github.axiom.ac.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link EventChannel}s, one per event type. This is the
 * GrimAPI-style integration surface: consumers obtain a channel with
 * {@link #channel(Class)} and subscribe to it.
 */
public final class EventBus {

    private final Map<Class<?>, EventChannel<?>> channels = new ConcurrentHashMap<>();

    /**
     * Returns the channel for {@code type}, creating it on first
     * request. The same channel instance is returned for every later
     * call with the same type.
     */
    @SuppressWarnings("unchecked")
    public <T> EventChannel<T> channel(Class<T> type) {
        return (EventChannel<T>) channels.computeIfAbsent(type, key -> new EventChannel<>());
    }

    /**
     * Publishes {@code event} to the channel of its runtime class.
     * If no channel exists for that class yet, the event is dropped.
     */
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        EventChannel<T> channel = (EventChannel<T>) channels.get(event.getClass());
        if (channel != null) {
            channel.publish(event);
        }
    }
}
