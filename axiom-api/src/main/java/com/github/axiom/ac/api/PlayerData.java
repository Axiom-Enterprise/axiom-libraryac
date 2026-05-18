package com.github.axiom.ac.api;

import com.github.axiom.ac.math.Vec3;
import java.util.UUID;

/**
 * Read-only view of a tracked player, inspected by {@link Check}s.
 * The concrete implementation is provided by the packet pipeline;
 * checks must treat this purely as a data source and must not call
 * the Bukkit API through it.
 */
public interface PlayerData {

    /** Unique id of the player. */
    UUID uuid();

    /** Current position. */
    Vec3 position();

    /** Current velocity (movement delta over the last tick). */
    Vec3 velocity();

    /** Current horizontal look angle, in degrees. */
    float yaw();

    /** Current vertical look angle, in degrees. */
    float pitch();

    /** True when the player is reported as standing on ground. */
    boolean onGround();
}
