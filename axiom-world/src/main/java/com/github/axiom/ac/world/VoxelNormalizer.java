package com.github.axiom.ac.world;

import com.github.axiom.ac.math.Vec3;

/**
 * Canonicalises raw world coordinates into voxel coordinates.
 *
 * <p>A client position is a {@code double} that has survived network
 * encoding and floating-point arithmetic. A coordinate that should sit
 * on an integer block boundary often arrives a hair off it — {@code
 * 4.9999998} for a true {@code 5.0} — and a bare {@link Math#floor}
 * then drops it into the wrong block. This normalizer snaps such
 * near-integer drift back to the boundary before flooring, so the same
 * physical position always resolves to the same {@link BlockPos}.
 *
 * <p>It also maps world coordinates into chunk-local space, the form
 * a chunk-section cache is keyed by.
 */
public final class VoxelNormalizer {

    /** Default snap tolerance: coordinates within this of an integer snap to it. */
    public static final double DEFAULT_EPSILON = 1.0e-4;

    /** Side length, in blocks, of a chunk column. */
    public static final int CHUNK_SIZE = 16;

    private VoxelNormalizer() {
    }

    /**
     * The block containing world position {@code v}, with each
     * component snapped to a near integer (within {@link
     * #DEFAULT_EPSILON}) before flooring.
     */
    public static BlockPos blockAt(Vec3 v) {
        return blockAt(v, DEFAULT_EPSILON);
    }

    /**
     * The block containing world position {@code v}, with each
     * component snapped to a near integer (within {@code epsilon})
     * before flooring.
     *
     * @param v       the world position
     * @param epsilon the snap tolerance; must not be negative
     */
    public static BlockPos blockAt(Vec3 v, double epsilon) {
        return new BlockPos(
                floorSnapped(v.x(), epsilon),
                floorSnapped(v.y(), epsilon),
                floorSnapped(v.z(), epsilon));
    }

    /**
     * Snaps {@code coord} to the nearest integer when it lies within
     * {@code epsilon} of one, and returns it unchanged otherwise. Kills
     * the float drift that would otherwise straddle a block boundary.
     *
     * @param epsilon the snap tolerance; must not be negative
     */
    public static double snap(double coord, double epsilon) {
        if (epsilon < 0.0) {
            throw new IllegalArgumentException("epsilon must not be negative");
        }
        double nearest = Math.rint(coord);
        return Math.abs(coord - nearest) <= epsilon ? nearest : coord;
    }

    /**
     * The chunk-local index ({@code 0}..{@code 15}) of a world
     * coordinate. Correct for negative coordinates: world {@code -1}
     * maps to local {@code 15}.
     */
    public static int chunkLocal(int worldCoord) {
        return worldCoord & (CHUNK_SIZE - 1);
    }

    /**
     * The index of the chunk a world coordinate falls in. Correct for
     * negative coordinates: world {@code -1} falls in chunk {@code -1}.
     */
    public static int chunkOf(int worldCoord) {
        return worldCoord >> 4;
    }

    /**
     * {@code pos} expressed with its X and Z folded into chunk-local
     * space ({@code 0}..{@code 15}); the Y coordinate is left as is.
     */
    public static BlockPos chunkLocal(BlockPos pos) {
        return new BlockPos(chunkLocal(pos.x()), pos.y(), chunkLocal(pos.z()));
    }

    private static int floorSnapped(double coord, double epsilon) {
        return (int) Math.floor(snap(coord, epsilon));
    }
}
