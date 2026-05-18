package com.github.axiom.ac.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import org.junit.jupiter.api.Test;

class MovementUpdateTest {

    @Test
    void fullUpdateCarriesPositionAndRotation() {
        MovementUpdate u = MovementUpdate.full(new Vec3(1, 64, 2), 90.0f, -10.0f, true);
        assertTrue(u.hasPosition());
        assertEquals(new Vec3(1, 64, 2), u.position());
        assertTrue(u.hasRotation());
        assertEquals(90.0f, u.yaw());
        assertEquals(-10.0f, u.pitch());
        assertTrue(u.onGround());
    }

    @Test
    void positionOnlyHasNoRotation() {
        MovementUpdate u = MovementUpdate.positionOnly(new Vec3(1, 64, 2), false);
        assertTrue(u.hasPosition());
        assertFalse(u.hasRotation());
        assertFalse(u.onGround());
    }

    @Test
    void rotationOnlyHasNoPosition() {
        MovementUpdate u = MovementUpdate.rotationOnly(45.0f, 5.0f, true);
        assertFalse(u.hasPosition());
        assertNull(u.position());
        assertTrue(u.hasRotation());
        assertEquals(45.0f, u.yaw());
    }

    @Test
    void groundOnlyHasNeitherPositionNorRotation() {
        MovementUpdate u = MovementUpdate.groundOnly(true);
        assertFalse(u.hasPosition());
        assertFalse(u.hasRotation());
        assertTrue(u.onGround());
    }
}
