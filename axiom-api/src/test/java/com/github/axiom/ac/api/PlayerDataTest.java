package com.github.axiom.ac.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerDataTest {

    /** Minimal test double proving the PlayerData contract is implementable. */
    private record FakePlayerData(UUID uuid, Vec3 position, Vec3 velocity,
                                  float yaw, float pitch, boolean onGround)
            implements PlayerData {
    }

    @Test
    void implementationExposesState() {
        UUID id = UUID.randomUUID();
        PlayerData data = new FakePlayerData(
                id, new Vec3(1, 64, 2), new Vec3(0.1, 0, 0.1), 90.0f, -10.0f, true);

        assertEquals(id, data.uuid());
        assertEquals(new Vec3(1, 64, 2), data.position());
        assertEquals(new Vec3(0.1, 0, 0.1), data.velocity());
        assertEquals(90.0f, data.yaw());
        assertEquals(-10.0f, data.pitch());
        assertTrue(data.onGround());
    }
}
