package com.github.axiom.ac.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerDataImplTest {

    private static final double EPS = 1e-9;

    @Test
    void rejectsNullUuid() {
        assertThrows(NullPointerException.class, () -> new PlayerDataImpl(null));
    }

    @Test
    void exposesUuid() {
        UUID id = UUID.randomUUID();
        assertEquals(id, new PlayerDataImpl(id).uuid());
    }

    @Test
    void positionUpdateRecomputesVelocity() {
        PlayerDataImpl data = new PlayerDataImpl(UUID.randomUUID());
        data.applyMovement(MovementUpdate.positionOnly(new Vec3(0, 64, 0), true));
        data.applyMovement(MovementUpdate.positionOnly(new Vec3(0.5, 64, 0.25), true));

        assertEquals(new Vec3(0.5, 64, 0.25), data.position());
        assertEquals(new Vec3(0, 64, 0), data.previousPosition());
        assertEquals(0.5, data.velocity().x(), EPS);
        assertEquals(0.0, data.velocity().y(), EPS);
        assertEquals(0.25, data.velocity().z(), EPS);
    }

    @Test
    void positionLessPacketResetsVelocity() {
        PlayerDataImpl data = new PlayerDataImpl(UUID.randomUUID());
        data.applyMovement(MovementUpdate.positionOnly(new Vec3(0, 64, 0), true));
        data.applyMovement(MovementUpdate.positionOnly(new Vec3(0.5, 64, 0), true));
        data.applyMovement(MovementUpdate.rotationOnly(45.0f, 0.0f, true));

        assertEquals(new Vec3(0, 0, 0), data.velocity());
        assertEquals(new Vec3(0.5, 64, 0), data.position());
    }

    @Test
    void rotationUpdateDoesNotTouchPosition() {
        PlayerDataImpl data = new PlayerDataImpl(UUID.randomUUID());
        data.applyMovement(MovementUpdate.positionOnly(new Vec3(1, 64, 1), true));
        data.applyMovement(MovementUpdate.rotationOnly(90.0f, -20.0f, true));

        assertEquals(new Vec3(1, 64, 1), data.position());
        assertEquals(90.0f, data.yaw());
        assertEquals(-20.0f, data.pitch());
    }

    @Test
    void groundFlagAlwaysApplied() {
        PlayerDataImpl data = new PlayerDataImpl(UUID.randomUUID());
        data.applyMovement(MovementUpdate.groundOnly(true));
        assertTrue(data.onGround());
        data.applyMovement(MovementUpdate.groundOnly(false));
        assertEquals(false, data.onGround());
    }

    @Test
    void positionHistoryGrowsWithPositionUpdates() {
        PlayerDataImpl data = new PlayerDataImpl(UUID.randomUUID());
        data.applyMovement(MovementUpdate.positionOnly(new Vec3(0, 64, 0), true));
        data.applyMovement(MovementUpdate.positionOnly(new Vec3(1, 64, 0), true));
        data.applyMovement(MovementUpdate.rotationOnly(10.0f, 0.0f, true));

        assertEquals(2, data.positionHistory().size());
    }
}
