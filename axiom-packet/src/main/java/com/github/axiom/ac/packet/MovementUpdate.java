package com.github.axiom.ac.packet;

import com.github.axiom.ac.math.Vec3;

/**
 * One decoded client movement packet. A Minecraft movement packet
 * may carry a position, a rotation, both, or only a ground flag.
 *
 * <p>When {@code hasPosition} is {@code false}, {@code position} is
 * {@code null}; consumers must check {@code hasPosition} first.
 *
 * @param hasPosition true when the packet carried a position
 * @param position    new position, or {@code null} when absent
 * @param hasRotation true when the packet carried a look angle
 * @param yaw         new horizontal look angle, in degrees
 * @param pitch       new vertical look angle, in degrees
 * @param onGround    client-reported ground state
 */
public record MovementUpdate(boolean hasPosition, Vec3 position,
                             boolean hasRotation, float yaw, float pitch,
                             boolean onGround) {

    /** A packet that carried both a position and a rotation. */
    public static MovementUpdate full(Vec3 position, float yaw, float pitch,
                                      boolean onGround) {
        return new MovementUpdate(true, position, true, yaw, pitch, onGround);
    }

    /** A packet that carried only a position. */
    public static MovementUpdate positionOnly(Vec3 position, boolean onGround) {
        return new MovementUpdate(true, position, false, 0.0f, 0.0f, onGround);
    }

    /** A packet that carried only a rotation. */
    public static MovementUpdate rotationOnly(float yaw, float pitch,
                                              boolean onGround) {
        return new MovementUpdate(false, null, true, yaw, pitch, onGround);
    }

    /** A packet that carried only a ground flag. */
    public static MovementUpdate groundOnly(boolean onGround) {
        return new MovementUpdate(false, null, false, 0.0f, 0.0f, onGround);
    }
}
