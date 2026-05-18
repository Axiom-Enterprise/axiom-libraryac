package com.github.axiom.ac.core;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AxiomProviderTest {

    @AfterEach
    void reset() {
        AxiomProvider.clear();
    }

    @Test
    void getIsEmptyBeforeARuntimeIsSet() {
        assertTrue(AxiomProvider.get().isEmpty());
    }

    @Test
    void getReturnsTheSetRuntime() {
        AxiomRuntime runtime = new AxiomRuntime(new MemoryStorageProvider());
        AxiomProvider.set(runtime);
        assertSame(runtime, AxiomProvider.get().orElseThrow());
    }

    @Test
    void clearRemovesTheRuntime() {
        AxiomProvider.set(new AxiomRuntime(new MemoryStorageProvider()));
        AxiomProvider.clear();
        assertTrue(AxiomProvider.get().isEmpty());
    }
}
