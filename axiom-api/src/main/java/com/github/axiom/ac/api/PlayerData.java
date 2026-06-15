package com.github.axiom.ac.api;

import com.github.axiom.ac.math.Rotation;
import com.github.axiom.ac.math.Vec3;
import java.util.List;
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

    /** The current look angle as a {@link Rotation}. */
    default Rotation rotation() {
        return new Rotation(yaw(), pitch());
    }

    /**
     * The look angle before the most recent rotation change. The
     * default — for a snapshot with no history — returns the current
     * rotation; the packet-pipeline implementation overrides it with
     * the genuine previous angle.
     */
    default Rotation previousRotation() {
        return rotation();
    }

    /**
     * Recent look angles, oldest first. The default returns just the
     * current rotation; the packet-pipeline implementation overrides
     * it with the bounded rotation history that aim analysis needs.
     */
    default List<Rotation> rotationHistory() {
        return List.of(rotation());
    }
}
