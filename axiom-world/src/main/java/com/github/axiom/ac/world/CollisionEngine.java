package com.github.axiom.ac.world;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.Ray;
import com.github.axiom.ac.math.Vec3;
import java.util.Objects;
import java.util.Optional;

/**
 * Queries collisions between geometry and the cached world. Each
 * block contributes its own collision shape.
 *
 * <p>Every query box is run through {@link AabbNormalizer} first, so a
 * box that arrives with an inverted axis still resolves correctly, and
 * the cell range it spans is taken with {@link VoxelNormalizer}, whose
 * near-integer snapping keeps a face resting on a block boundary from
 * landing in the wrong cell through floating-point drift.
 */
public final class CollisionEngine {

    private final WorldCache world;

    public CollisionEngine(WorldCache world) {
        this.world = Objects.requireNonNull(world, "world");
    }

    /** The world cache this engine queries. */
    public WorldCache world() {
        return world;
    }

    /**
     * True when {@code box} overlaps the collision shape of any
     * cached block. The box is normalized first, so an inverted axis
     * is repaired rather than silently missed. Each block contributes
     * its own cell-local boxes (a full cube, a slab half, and so on),
     * translated into world space; a box touched only at a shared face
     * does not count, matching {@link Aabb}'s strict-overlap rule.
     */
    public boolean collides(Aabb box) {
        Aabb query = AabbNormalizer.normalize(box);
        BlockPos min = VoxelNormalizer.blockAt(
                new Vec3(query.minX(), query.minY(), query.minZ()));
        BlockPos max = VoxelNormalizer.blockAt(
                new Vec3(query.maxX(), query.maxY(), query.maxZ()));
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    BlockState state = world.blockAt(new BlockPos(x, y, z));
                    for (Aabb local : state.collisionBoxes()) {
                        Aabb worldBox = local.offset(x, y, z);
                        if (query.intersects(worldBox)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Traverses {@code ray} through the voxel grid and returns the
     * first solid block whose entry point lies within
     * {@code maxDistance} world units of the ray origin, or empty
     * when none is hit. The voxel containing the origin counts as
     * entered at distance 0. Uses Amanatides–Woo voxel traversal.
     */
    public Optional<BlockPos> raycast(Ray ray, double maxDistance) {
        Vec3 direction = ray.direction();
        double length = direction.length();
        if (length == 0.0) {
            return Optional.empty();
        }
        Vec3 dir = direction.scale(1.0 / length);
        Vec3 origin = ray.origin();

        BlockPos originVoxel = VoxelNormalizer.blockAt(origin);
        int x = originVoxel.x();
        int y = originVoxel.y();
        int z = originVoxel.z();

        if (world.isSolid(originVoxel)) {
            return Optional.of(originVoxel);
        }


        int stepX = signum(dir.x());
        int stepY = signum(dir.y());
        int stepZ = signum(dir.z());

        double tMaxX = boundaryDistance(origin.x(), dir.x(), stepX);
        double tMaxY = boundaryDistance(origin.y(), dir.y(), stepY);
        double tMaxZ = boundaryDistance(origin.z(), dir.z(), stepZ);

        double tDeltaX = dir.x() == 0.0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.x());
        double tDeltaY = dir.y() == 0.0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.y());
        double tDeltaZ = dir.z() == 0.0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.z());

        while (true) {
            double travelled;
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
            if (travelled > maxDistance) {
                return Optional.empty();
            }
            BlockPos voxel = new BlockPos(x, y, z);
            if (world.isSolid(voxel)) {
                return Optional.of(voxel);
            }
        }
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
