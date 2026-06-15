package com.github.axiom.ac.world;

import com.github.axiom.ac.math.Aabb;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Canonicalises a block's collision shape — the list of cell-local
 * boxes a {@link BlockState} carries.
 *
 * <p>A shape reconstructed from voxel-shape data can contain boxes
 * that are degenerate (zero thickness on some axis), redundant
 * (duplicated, or wholly inside another box), or out of the unit
 * cell. None of those change what the shape collides with, but they
 * waste work in {@link CollisionEngine} and break value comparison of
 * two equivalent shapes. Normalizing yields the minimal, deterministic
 * box list every equivalent shape shares.
 *
 * <p>The normalizer does <em>not</em> merge adjacent boxes into larger
 * ones: the input boxes are kept, only filtered and ordered.
 */
public final class ShapeNormalizer {

    private ShapeNormalizer() {
    }

    private static final Comparator<Aabb> ORDER = Comparator
            .comparingDouble(Aabb::minX).thenComparingDouble(Aabb::minY)
            .thenComparingDouble(Aabb::minZ).thenComparingDouble(Aabb::maxX)
            .thenComparingDouble(Aabb::maxY).thenComparingDouble(Aabb::maxZ);

    /**
     * Returns the canonical form of {@code boxes}: each box ordered
     * and clamped cell-local, every degenerate or duplicate box
     * dropped, every box wholly contained in another dropped, and the
     * survivors sorted into a deterministic order.
     *
     * @param boxes the raw collision boxes; not modified
     * @return a new, canonical, immutable box list
     */
    public static List<Aabb> normalize(List<Aabb> boxes) {
        Objects.requireNonNull(boxes, "boxes");

        LinkedHashSet<Aabb> distinct = new LinkedHashSet<>();
        for (Aabb box : boxes) {
            Aabb local = AabbNormalizer.cellLocal(box);
            if (!isDegenerate(local)) {
                distinct.add(local);
            }
        }

        List<Aabb> minimal = new ArrayList<>();
        for (Aabb box : distinct) {
            boolean redundant = false;
            for (Aabb other : distinct) {
                if (other != box && contains(other, box)) {
                    redundant = true;
                    break;
                }
            }
            if (!redundant) {
                minimal.add(box);
            }
        }

        minimal.sort(ORDER);
        return List.copyOf(minimal);
    }

    /**
     * True when {@code boxes} is already in canonical form — equal to
     * its own normalization.
     */
    public static boolean isCanonical(List<Aabb> boxes) {
        return normalize(boxes).equals(boxes);
    }

    /** True when {@code box} has no volume: zero or negative on any axis. */
    private static boolean isDegenerate(Aabb box) {
        return box.minX() >= box.maxX()
                || box.minY() >= box.maxY()
                || box.minZ() >= box.maxZ();
    }

    /** True when {@code outer} wholly encloses {@code inner}. */
    private static boolean contains(Aabb outer, Aabb inner) {
        return outer.minX() <= inner.minX() && outer.maxX() >= inner.maxX()
                && outer.minY() <= inner.minY() && outer.maxY() >= inner.maxY()
                && outer.minZ() <= inner.minZ() && outer.maxZ() >= inner.maxZ();
    }
}
