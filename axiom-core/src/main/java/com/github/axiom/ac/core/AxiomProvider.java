package com.github.axiom.ac.core;

import java.util.Objects;
import java.util.Optional;

/**
 * Static holder for the shared {@link AxiomRuntime}. The plugin sets
 * the runtime on enable and clears it on disable; integrating
 * plugins read it through {@link #get()}.
 */
public final class AxiomProvider {

    private static volatile AxiomRuntime runtime;

    private AxiomProvider() {
    }

    /** Sets the shared runtime instance. */
    public static void set(AxiomRuntime runtime) {
        AxiomProvider.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    /** The shared runtime, or empty when none has been set. */
    public static Optional<AxiomRuntime> get() {
        return Optional.ofNullable(runtime);
    }

    /** Clears the shared runtime. */
    public static void clear() {
        runtime = null;
    }
}
