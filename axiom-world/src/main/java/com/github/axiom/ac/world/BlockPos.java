package com.github.axiom.ac.world;

import com.github.axiom.ac.math.Vec3;

/**
 * Immutable integer block coordinate.
 *
 * @param x block x
 * @param y block y
 * @param z block z
 */
public record BlockPos(int x, int y, int z) {

    /**
     * Returns the block that contains world position {@code v},
     * flooring each component.
     */
    public static BlockPos of(Vec3 v) {
        return new BlockPos(
                (int) Math.floor(v.x()),
                (int) Math.floor(v.y()),
                (int) Math.floor(v.z()));
    }
}
