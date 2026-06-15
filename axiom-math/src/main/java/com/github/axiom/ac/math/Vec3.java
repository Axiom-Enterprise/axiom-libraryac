package com.github.axiom.ac.math;

/**
 * Immutable 3D vector of double components. Building block for geometry
 * and physics math.
 */
public record Vec3(double x, double y, double z) {

    /** The zero vector. */
    public static final Vec3 ZERO = new Vec3(0, 0, 0);

    public Vec3 add(Vec3 other) {
        return new Vec3(x + other.x, y + other.y, z + other.z);
    }

    public Vec3 subtract(Vec3 other) {
        return new Vec3(x - other.x, y - other.y, z - other.z);
    }

    public Vec3 scale(double factor) {
        return new Vec3(x * factor, y * factor, z * factor);
    }

    public double dot(Vec3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public double distanceSquared(Vec3 other) {
        return subtract(other).lengthSquared();
    }

    public double distance(Vec3 other) {
        return Math.sqrt(distanceSquared(other));
    }

    /** Returns a unit vector in the same direction, or zero if this is zero. */
    public Vec3 normalize() {
        double len = length();
        return len == 0.0 ? this : scale(1.0 / len);
    }

    /**
     * Returns this vector shortened to {@code maxLength} when it is
     * longer, and unchanged otherwise. Used to cap a velocity at a
     * physically plausible bound without altering its direction.
     *
     * @param maxLength the longest allowed magnitude; must not be
     *                  negative
     */
    public Vec3 clampLength(double maxLength) {
        if (maxLength < 0.0) {
            throw new IllegalArgumentException("maxLength must not be negative");
        }
        double len = length();
        return len <= maxLength || len == 0.0 ? this : scale(maxLength / len);
    }
}
