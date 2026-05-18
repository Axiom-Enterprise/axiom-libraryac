package com.github.axiom.ac.api.event;

import java.util.Objects;

/**
 * Fired when a check is auto-disabled after repeatedly throwing
 * exceptions during inspection.
 *
 * @param checkId id of the faulted check
 * @param reason  human-readable explanation of the fault
 */
public record CheckFaultEvent(String checkId, String reason) {

    public CheckFaultEvent {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(reason, "reason");
    }
}
