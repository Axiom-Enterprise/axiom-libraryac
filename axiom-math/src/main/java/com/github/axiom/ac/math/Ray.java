package com.github.axiom.ac.math;

import java.util.OptionalDouble;

/**
 * A ray defined by an origin and a direction. Used for reach and
 * line-of-sight math against {@link Aabb} hitboxes.
 */
public record Ray(Vec3 origin, Vec3 direction) {

    /**
     * Intersects this ray with {@code box} using the slab method.
     * Returns the distance {@code t} (in units of {@code direction}
     * length) to the first intersection, or empty when the ray
     * misses or only hits behind the origin. When the origin is
     * inside the box, returns 0.
     */
    public OptionalDouble intersect(Aabb box) {
        double[] o = {origin.x(), origin.y(), origin.z()};
        double[] d = {direction.x(), direction.y(), direction.z()};
        double[] lo = {box.minX(), box.minY(), box.minZ()};
        double[] hi = {box.maxX(), box.maxY(), box.maxZ()};

        double tMin = Double.NEGATIVE_INFINITY;
        double tMax = Double.POSITIVE_INFINITY;

        for (int i = 0; i < 3; i++) {
            if (d[i] == 0.0) {
                if (o[i] < lo[i] || o[i] > hi[i]) {
                    return OptionalDouble.empty();
                }
            } else {
                double t1 = (lo[i] - o[i]) / d[i];
                double t2 = (hi[i] - o[i]) / d[i];
                if (t1 > t2) {
                    double tmp = t1;
                    t1 = t2;
                    t2 = tmp;
                }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                if (tMin > tMax) {
                    return OptionalDouble.empty();
                }
            }
        }
        if (tMax < 0.0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(tMin >= 0.0 ? tMin : 0.0);
    }
}
