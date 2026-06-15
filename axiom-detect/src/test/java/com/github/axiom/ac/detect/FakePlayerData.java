package com.github.axiom.ac.detect;

import com.github.axiom.ac.api.PlayerData;
import com.github.axiom.ac.math.Vec3;
import java.util.UUID;

/**
 * Minimal {@link PlayerData} for tests — a plain immutable snapshot,
 * with no packet pipeline behind it.
 */
public record FakePlayerData(UUID uuid, Vec3 position, Vec3 velocity,
                             float yaw, float pitch, boolean onGround) implements PlayerData {

    /** A snapshot at {@code position} with zero velocity, looking north on the ground. */
    public static FakePlayerData at(UUID uuid, Vec3 position) {
        return new FakePlayerData(uuid, position, new Vec3(0, 0, 0), 0.0f, 0.0f, true);
    }
}
