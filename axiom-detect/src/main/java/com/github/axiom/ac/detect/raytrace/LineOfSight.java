package com.github.axiom.ac.detect.raytrace;

import com.github.axiom.ac.math.Ray;
import com.github.axiom.ac.math.Vec3;
import com.github.axiom.ac.world.CollisionEngine;
import java.util.Objects;

/**
 * Tests whether the straight segment between two points is free of
 * solid blocks — the visibility half of an aim or reach check. It
 * walks the cached world with the {@link CollisionEngine}'s voxel
 * raycast; a solid block before the destination breaks the line.
 *
 * <p>Resolution is whole-block: every solid block is a full cube and
 * the endpoint's own block is not treated as an obstruction.
 */
public final class LineOfSight {

    private LineOfSight() {
    }

    /**
     * True when no cached solid block lies between {@code from} and
     * {@code to}. Coincident points are trivially clear.
     */
    public static boolean clear(Vec3 from, Vec3 to, CollisionEngine collision) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(collision, "collision");

        Vec3 delta = to.subtract(from);
        double distance = delta.length();
        if (distance == 0.0) {
            return true;
        }
        return collision.raycast(new Ray(from, delta), distance).isEmpty();
    }
}
