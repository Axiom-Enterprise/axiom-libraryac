package com.github.axiom.ac.api;

/**
 * Mix-in for events that a subscriber can cancel. A cancelled event
 * signals that its default consequence (for example a punishment)
 * should not happen.
 */
public interface Cancellable {

    /** True when a subscriber has cancelled this event. */
    boolean isCancelled();

    /** Sets the cancelled state of this event. */
    void setCancelled(boolean cancelled);
}
