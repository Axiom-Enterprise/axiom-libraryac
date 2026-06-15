package com.github.axiom.ac.world;

import com.github.axiom.ac.math.Aabb;

/**
 * Canonicalises axis-aligned bounding boxes for collision queries.
 *
 * <p>{@link Aabb} carries no constructor validation: a box decoded
 * from packet or chunk data can arrive with a minimum corner past its
 * maximum, or with components outside the unit cell a collision shape
 * must occupy. Querying such a box gives silently wrong results —
 * {@link Aabb#intersects} assumes {@code min <= max}. This normalizer
 * repairs both faults before a box reaches {@link CollisionEngine}.
 */
public final class AabbNormalizer {

    private AabbNormalizer() {
    }

    /**
     * Returns {@code box} with every axis ordered so the minimum
     * corner does not exceed the maximum, swapping any inverted pair.
     * A box already ordered is returned unchanged.
     */
    public static Aabb normalize(Aabb box) {
        if (isCanonical(box)) {
            return box;
        }
        return new Aabb(
                Math.min(box.minX(), box.maxX()),
                Math.min(box.minY(), box.maxY()),
                Math.min(box.minZ(), box.maxZ()),
                Math.max(box.minX(), box.maxX()),
                Math.max(box.minY(), box.maxY()),
                Math.max(box.minZ(), box.maxZ()));
    }

    /** True when every axis of {@code box} has its minimum at or below its maximum. */
    public static boolean isCanonical(Aabb box) {
        return box.minX() <= box.maxX()
                && box.minY() <= box.maxY()
                && box.minZ() <= box.maxZ();
    }

    /**
     * Returns {@code box} ordered and clamped into the unit cell
     * {@code [0, 1]} on every axis — the form a {@link BlockState}
     * collision shape must take. Components already in range are left
     * untouched.
     */
    public static Aabb cellLocal(Aabb box) {
        Aabb ordered = normalize(box);
        return new Aabb(
                clampUnit(ordered.minX()), clampUnit(ordered.minY()),
                clampUnit(ordered.minZ()), clampUnit(ordered.maxX()),
                clampUnit(ordered.maxY()), clampUnit(ordered.maxZ())
        );
    }

    /**
     * True when {@code box} is ordered and lies wholly within the unit
     * cell — already a valid cell-local collision shape.
     */
    public static boolean isCellLocal(Aabb box) {
        return isCanonical(box)
                && box.minX() >= 0.0 && box.maxX() <= 1.0
                && box.minY() >= 0.0 && box.maxY() <= 1.0
                && box.minZ() >= 0.0 && box.maxZ() <= 1.0;
    }

    private static double clampUnit(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
