package com.github.axiom.ac.world;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.Ray;
import com.github.axiom.ac.math.Vec3;
import java.util.Objects;
import java.util.Optional;

/**
 * Queries collisions between geometry and the cached world. Every
 * solid block is treated as a full unit cube.
 */
public final class CollisionEngine {

    private final WorldCache world;

    public CollisionEngine(WorldCache world) {
        this.world = Objects.requireNonNull(world, "world");
    }

    /**
     * True when {@code box} overlaps any solid block. A block touched
     * only at a shared face does not count, matching {@link Aabb}'s
     * strict-overlap rule.
     */
    public boolean collides(Aabb box) {
        int minX = (int) Math.floor(box.minX());
        int minY = (int) Math.floor(box.minY());
        int minZ = (int) Math.floor(box.minZ());
        int maxX = (int) Math.ceil(box.maxX()) - 1;
        int maxY = (int) Math.ceil(box.maxY()) - 1;
        int maxZ = (int) Math.ceil(box.maxZ()) - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.isSolid(new BlockPos(x, y, z))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Traverses {@code ray} through the voxel grid and returns the
     * first solid block within {@code maxDistance} world units, or
     * empty when none is hit. Uses Amanatides–Woo voxel traversal.
     */
    public Optional<BlockPos> raycast(Ray ray, double maxDistance) {
        Vec3 direction = ray.direction();
        double length = direction.length();
        if (length == 0.0) {
            return Optional.empty();
        }
        Vec3 dir = direction.scale(1.0 / length);
        Vec3 origin = ray.origin();

        int x = (int) Math.floor(origin.x());
        int y = (int) Math.floor(origin.y());
        int z = (int) Math.floor(origin.z());

        int stepX = signum(dir.x());
        int stepY = signum(dir.y());
        int stepZ = signum(dir.z());

        double tMaxX = boundaryDistance(origin.x(), dir.x(), stepX);
        double tMaxY = boundaryDistance(origin.y(), dir.y(), stepY);
        double tMaxZ = boundaryDistance(origin.z(), dir.z(), stepZ);

        double tDeltaX = dir.x() == 0.0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.x());
        double tDeltaY = dir.y() == 0.0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.y());
        double tDeltaZ = dir.z() == 0.0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.z());

        double travelled = 0.0;
        while (travelled <= maxDistance) {
            if (world.isSolid(new BlockPos(x, y, z))) {
                return Optional.of(new BlockPos(x, y, z));
            }
            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                travelled = tMaxX;
                tMaxX += tDeltaX;
                x += stepX;
            } else if (tMaxY <= tMaxZ) {
                travelled = tMaxY;
                tMaxY += tDeltaY;
                y += stepY;
            } else {
                travelled = tMaxZ;
                tMaxZ += tDeltaZ;
                z += stepZ;
            }
        }
        return Optional.empty();
    }

    private static int signum(double value) {
        if (value > 0.0) {
            return 1;
        }
        if (value < 0.0) {
            return -1;
        }
        return 0;
    }

    /**
     * Distance along the ray from {@code coordinate} to the first
     * voxel boundary in the stepping direction.
     */
    private static double boundaryDistance(double coordinate, double dir, int step) {
        if (step == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double current = Math.floor(coordinate);
        double next = step > 0 ? current + 1.0 : current;
        return (next - coordinate) / dir;
    }
}
