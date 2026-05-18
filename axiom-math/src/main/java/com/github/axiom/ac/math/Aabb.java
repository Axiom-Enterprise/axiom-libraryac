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
}
