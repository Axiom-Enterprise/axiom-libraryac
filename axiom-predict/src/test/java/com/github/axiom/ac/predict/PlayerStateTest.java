package com.github.axiom.ac.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import org.junit.jupiter.api.Test;

class PlayerStateTest {

    @Test
    void exposesItsFields() {
        PlayerState state = new PlayerState(
                new Vec3(1, 64, 2), new Vec3(0.1, 0, 0.2), 90.0f, true);
        assertEquals(new Vec3(1, 64, 2), state.position());
        assertEquals(new Vec3(0.1, 0, 0.2), state.velocity());
        assertEquals(90.0f, state.yaw());
        assertTrue(state.onGround());
    }

    @Test
    void rejectsNullPosition() {
        assertThrows(NullPointerException.class,
                () -> new PlayerState(null, new Vec3(0, 0, 0), 0.0f, false));
    }

    @Test
    void rejectsNullVelocity() {
        assertThrows(NullPointerException.class,
                () -> new PlayerState(new Vec3(0, 0, 0), null, 0.0f, false));
    }

    @Test
    void legacyConstructorLeavesPitchLevel() {
        PlayerState state = new PlayerState(new Vec3(0, 0, 0), new Vec3(0, 0, 0),
                45.0f, true);
        assertEquals(0.0f, state.pitch());
    }

    @Test
    void exposesPitchAndRotation() {
        PlayerState state = new PlayerState(new Vec3(0, 0, 0), new Vec3(0, 0, 0),
                90.0f, -30.0f, true);
        assertEquals(-30.0f, state.pitch());
        assertEquals(90.0f, state.rotation().yaw());
        assertEquals(-30.0f, state.rotation().pitch());
    }
}
