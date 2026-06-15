package com.github.axiom.ac.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MovementInputTest {

    @Test
    void exposesItsFields() {
        MovementInput input = new MovementInput(1, -1, true, false);
        assertEquals(1, input.forward());
        assertEquals(-1, input.strafe());
        assertEquals(true, input.jump());
        assertEquals(false, input.sprint());
    }

    @Test
    void noneIsAllZeroAndUnpressed() {
        MovementInput none = MovementInput.none();
        assertEquals(0, none.forward());
        assertEquals(0, none.strafe());
        assertFalse(none.jump());
        assertFalse(none.sprint());
    }

    @Test
    void rejectsForwardOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new MovementInput(2, 0, false, false));
    }

    @Test
    void rejectsStrafeOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new MovementInput(0, -2, false, false));
    }

    @Test
    void legacyConstructorLeavesSneakUnpressed() {
        assertFalse(new MovementInput(1, 0, false, false).sneak());
    }

    @Test
    void exposesSneak() {
        assertEquals(true, new MovementInput(0, 0, false, false, true).sneak());
    }

    @Test
    void isSprintingForwardNeedsBothSprintAndForward() {
        assertFalse(new MovementInput(0, 1, false, true).isSprintingForward());
        assertFalse(new MovementInput(1, 0, false, false).isSprintingForward());
        assertFalse(new MovementInput(-1, 0, false, true).isSprintingForward());
        assertEquals(true, new MovementInput(1, 0, false, true).isSprintingForward());
    }

    @Test
    void hasDirectionalInputDetectsAnyAxis() {
        assertFalse(MovementInput.none().hasDirectionalInput());
        assertEquals(true, new MovementInput(0, -1, false, false).hasDirectionalInput());
    }
}
