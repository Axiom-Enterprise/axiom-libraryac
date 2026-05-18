package com.github.axiom.ac.math;

/**
 * Immutable 3D vector of double components. Building block for geometry
 * and physics math.
 */
public record Vec3(double x, double y, double z) {

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
}
