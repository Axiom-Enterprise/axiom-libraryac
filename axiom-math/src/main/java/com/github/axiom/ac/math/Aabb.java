package com.github.axiom.ac.math;

/**
 * Immutable axis-aligned bounding box, defined by its minimum and
 * maximum corner. Used for collision and hitbox math.
 */
public record Aabb(double minX, double minY, double minZ,
                    double maxX, double maxY, double maxZ) {

    /** True when this box overlaps {@code other}; shared faces do not count. */
    public boolean intersects(Aabb other) {
        return minX < other.maxX && maxX > other.minX
            && minY < other.maxY && maxY > other.minY
            && minZ < other.maxZ && maxZ > other.minZ;
    }

    /** True when {@code point} lies inside or on this box. */
    public boolean contains(Vec3 point) {
        return point.x() >= minX && point.x() <= maxX
            && point.y() >= minY && point.y() <= maxY
            && point.z() >= minZ && point.z() <= maxZ;
    }

    /** Grows the box outward by the given amount on each axis. */
    public Aabb expand(double dx, double dy, double dz) {
        return new Aabb(minX - dx, minY - dy, minZ - dz,
                        maxX + dx, maxY + dy, maxZ + dz);
    }

    /** Translates the box by the given offset. */
    public Aabb offset(double dx, double dy, double dz) {
        return new Aabb(minX + dx, minY + dy, minZ + dz,
                        maxX + dx, maxY + dy, maxZ + dz);
    }

    /**
     * The point on or inside this box nearest to {@code point}. When
     * {@code point} is already inside, it is returned unchanged. The
     * building block of reach (eye-to-hitbox) distance.
     */
    public Vec3 closestPoint(Vec3 point) {
        return new Vec3(
                clamp(point.x(), minX, maxX),
                clamp(point.y(), minY, maxY),
                clamp(point.z(), minZ, maxZ));
    }

    /** Squared distance from {@code point} to this box; 0 when inside. */
    public double distanceSquaredTo(Vec3 point) {
        return point.distanceSquared(closestPoint(point));
    }

    /** Distance from {@code point} to this box; 0 when inside. */
    public double distanceTo(Vec3 point) {
        return Math.sqrt(distanceSquaredTo(point));
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }
}
