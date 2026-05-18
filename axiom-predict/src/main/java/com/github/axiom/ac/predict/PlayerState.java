package com.github.axiom.ac.predict;

import com.github.axiom.ac.math.Vec3;
import java.util.Objects;

/**
 * Immutable snapshot of a player's movement state — the input and
 * output type of a prediction step.
 *
 * @param position feet position
 * @param velocity velocity (movement delta over the last tick)
 * @param yaw      horizontal look angle, in degrees
 * @param onGround whether the player is supported from below
 */
public record PlayerState(Vec3 position, Vec3 velocity, float yaw, boolean onGround) {

    public PlayerState {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(velocity, "velocity");
    }
}
