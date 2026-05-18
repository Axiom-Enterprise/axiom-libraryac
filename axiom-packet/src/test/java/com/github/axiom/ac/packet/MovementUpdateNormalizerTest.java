package com.github.axiom.ac.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import org.junit.jupiter.api.Test;

class MovementUpdateNormalizerTest {

    private static final double EPS = 1e-9;

    @Test
    void rejectsANullUpdate() {
        assertThrows(NullPointerException.class,
                () -> new MovementUpdateNormalizer().normalize(null));
    }

    @Test
    void aFullPacketPassesThroughComplete() {
        MovementUpdateNormalizer normalizer = new MovementUpdateNormalizer();
        MovementUpdate result = normalizer.normalize(
                MovementUpdate.full(new Vec3(1, 2, 3), 45.0f, 10.0f, true));

        assertTrue(result.hasPosition());
        assertTrue(result.hasRotation());
        assertEquals(new Vec3(1, 2, 3), result.position());
        assertEquals(45.0f, result.yaw(), EPS);
        assertTrue(result.onGround());
    }

    @Test
    void aGroundOnlyPacketKeepsTheLastPositionAndRotation() {
        MovementUpdateNormalizer normalizer = new MovementUpdateNormalizer();
        normalizer.normalize(MovementUpdate.full(new Vec3(5, 6, 7), 90.0f, 20.0f, true));

        MovementUpdate result = normalizer.normalize(MovementUpdate.groundOnly(false));

        assertTrue(result.hasPosition());
        assertTrue(result.hasRotation());
        assertEquals(new Vec3(5, 6, 7), result.position());
        assertEquals(90.0f, result.yaw(), EPS);
        assertEquals(20.0f, result.pitch(), EPS);
        assertFalse(result.onGround());
    }

    @Test
    void aPositionOnlyPacketCarriesTheRotationForward() {
        MovementUpdateNormalizer normalizer = new MovementUpdateNormalizer();
        normalizer.normalize(MovementUpdate.rotationOnly(30.0f, -15.0f, true));

        MovementUpdate result = normalizer.normalize(
                MovementUpdate.positionOnly(new Vec3(8, 9, 10), true));

        assertEquals(new Vec3(8, 9, 10), result.position());
        assertEquals(30.0f, result.yaw(), EPS);
        assertEquals(-15.0f, result.pitch(), EPS);
    }

    @Test
    void rotationIsCanonicalised() {
        MovementUpdateNormalizer normalizer = new MovementUpdateNormalizer();
        MovementUpdate result = normalizer.normalize(
                MovementUpdate.rotationOnly(450.0f, 130.0f, true));

        assertEquals(90.0f, result.yaw(), EPS);
        assertEquals(90.0f, result.pitch(), EPS);
    }

    @Test
    void tracksWhetherPositionAndRotationHaveBeenSeen() {
        MovementUpdateNormalizer normalizer = new MovementUpdateNormalizer();
        assertFalse(normalizer.hasPosition());
        assertFalse(normalizer.hasRotation());

        normalizer.normalize(MovementUpdate.positionOnly(new Vec3(1, 1, 1), true));
        assertTrue(normalizer.hasPosition());
        assertFalse(normalizer.hasRotation());
    }
}
