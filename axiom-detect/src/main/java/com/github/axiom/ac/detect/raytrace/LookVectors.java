package com.github.axiom.ac.detect.raytrace;

import com.github.axiom.ac.math.Vec3;

/**
 * Converts a player's yaw/pitch and feet position into the eye ray a
 * raytrace needs. The direction formula matches Minecraft's own
 * {@code Location.getDirection()}: yaw is measured clockwise from
 * south (+Z) and pitch downward, both in degrees.
 */
public final class LookVectors {

    /** Eye height of a standing player, in blocks. */
    public static final double STANDING_EYE_HEIGHT = 1.62;

    /** Eye height while sneaking, in blocks. */
    public static final double SNEAKING_EYE_HEIGHT = 1.27;

    private LookVectors() {
    }

    /** Unit look direction for the given yaw and pitch, in degrees. */
    public static Vec3 direction(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);
        return new Vec3(
                -cosPitch * Math.sin(yawRad),
                -Math.sin(pitchRad),
                cosPitch * Math.cos(yawRad));
    }

    /** Eye position {@code eyeHeight} blocks above a feet position. */
    public static Vec3 eyePosition(Vec3 feet, double eyeHeight) {
        return feet.add(new Vec3(0.0, eyeHeight, 0.0));
    }
}
