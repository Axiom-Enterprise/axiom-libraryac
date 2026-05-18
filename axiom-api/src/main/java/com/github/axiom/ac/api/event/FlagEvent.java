package com.github.axiom.ac.api.event;

import com.github.axiom.ac.api.Cancellable;
import com.github.axiom.ac.api.Violation;
import java.util.Objects;
import java.util.UUID;

/**
 * Fired when a check flags a player with a {@link Violation}.
 * Cancelling this event asks the runtime to suppress the default
 * consequence (for example a configured punishment).
 */
public final class FlagEvent implements Cancellable {

    private final UUID playerId;
    private final Violation violation;
    private boolean cancelled;

    public FlagEvent(UUID playerId, Violation violation) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.violation = Objects.requireNonNull(violation, "violation");
    }

    /** Id of the flagged player. */
    public UUID playerId() {
        return playerId;
    }

    /** The violation that triggered this event. */
    public Violation violation() {
        return violation;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
