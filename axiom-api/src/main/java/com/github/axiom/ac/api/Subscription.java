package com.github.axiom.ac.api;

/**
 * Handle to a single event subscription. Closing it removes the
 * handler from its {@link EventChannel}.
 */
@FunctionalInterface
public interface Subscription {

    /** Removes the associated handler from its channel. */
    void unsubscribe();
}
