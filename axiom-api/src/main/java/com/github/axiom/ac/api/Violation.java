package com.github.axiom.ac.api;

import java.util.Objects;

/**
 * The result a {@link Check} emits when it detects suspicious
 * behaviour.
 *
 * @param checkId     id of the check that produced this violation
 * @param description human-readable explanation
 * @param value       check-defined magnitude (for example an offset
 *                    or score); interpretation is up to the check
 * @param confidence  detection confidence, normalised to {@code [0, 1]}
 */
public record Violation(String checkId, String description,
                        double value, double confidence) {

    public Violation {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(description, "description");
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be in [0, 1]");
        }
    }
}
