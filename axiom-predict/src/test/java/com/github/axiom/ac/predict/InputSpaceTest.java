package com.github.axiom.ac.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InputSpaceTest {

    @Test
    void enumeratesEveryCombination() {
        // 3 forward * 3 strafe * 2 jump * 2 sprint.
        assertEquals(36, InputSpace.all().size());
    }

    @Test
    void everyInputIsDistinct() {
        List<MovementInput> all = InputSpace.all();
        assertEquals(all.size(), Set.copyOf(all).size());
    }

    @Test
    void includesTheNeutralInput() {
        assertTrue(InputSpace.all().contains(MovementInput.none()));
    }
}
