package com.github.axiom.ac.math;

/**
 * An immutable look angle: a {@code yaw} (horizontal) and a
 * {@code pitch} (vertical), both in degrees, following Minecraft's
 * conventions. Yaw 0 faces +Z and increases clockwise; pitch is -90
 * looking straight up and +90 straight down.
 *
 * <p>The building block of aim analysis: it converts an angle into a
 * unit look vector and measures the shortest signed rotation between
 * two angles.
 *
 * @param yaw   horizontal look angle, in degrees
 * @param pitch vertical look angle, in degrees
 */
public record Rotation(float yaw, float pitch) {

    /**
     * Normalises {@code degrees} to the half-open range
     * {@code [-180, 180)}. Used to take the shortest signed yaw step
     * between two angles, ignoring full-turn wrap-around.
     */
    public static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped >= 180.0) {
            wrapped -= 360.0;
        } else if (wrapped < -180.0) {
            wrapped += 360.0;
        }
        return wrapped;
    }

    /**
     * Shortest signed yaw change from this angle to {@code to}, in
     * degrees, in {@code [-180, 180)}. Positive turns clockwise.
     */
    public double yawDelta(Rotation to) {
        return wrapDegrees((double) to.yaw - this.yaw);
    }

    /** Signed pitch change from this angle to {@code to}, in degrees. */
    public double pitchDelta(Rotation to) {
        return (double) to.pitch - this.pitch;
    }

    /**
     * Combined magnitude of the yaw and pitch change to {@code to} —
     * the Euclidean norm of the two deltas, a single scalar for how
     * far the look angle moved.
     */
    public double magnitudeDelta(Rotation to) {
        return Math.hypot(yawDelta(to), pitchDelta(to));
    }

    /**
     * This angle in canonical form: the yaw wrapped into
     * {@code [-180, 180)} and the pitch clamped to {@code [-90, 90]},
     * the range a vanilla client can report. Raw packet angles drift
     * outside these bounds across full turns; normalizing first keeps
     * yaw and pitch deltas honest.
     */
    public Rotation normalized() {
        float wrappedYaw = (float) wrapDegrees(yaw);
        float clampedPitch = (float) Math.max(-90.0, Math.min(90.0, pitch));
        return wrappedYaw == yaw && clampedPitch == pitch
                ? this
                : new Rotation(wrappedYaw, clampedPitch);
    }

    /**
     * The unit vector this angle points along, in Minecraft's
     * coordinate frame.
     */
    public Vec3 directionVector() {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);
        return new Vec3(
                -Math.sin(yawRad) * cosPitch,
                -Math.sin(pitchRad),
                Math.cos(yawRad) * cosPitch);
    }

    /**
     * The angle, in degrees, between where this rotation points and
     * where {@code other} points — always in {@code [0, 180]}. Zero
     * when both face the same way.
     */
    public double angleTo(Rotation other) {
        double dot = directionVector().dot(other.directionVector());
        dot = Math.max(-1.0, Math.min(1.0, dot));
        return Math.toDegrees(Math.acos(dot));
    }
}
