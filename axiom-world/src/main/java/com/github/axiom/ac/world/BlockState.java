package com.github.axiom.ac.world;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.MotionFormulas;
import java.util.List;
import java.util.Objects;

/**
 * Collision state of a cached block: its collision shape and its
 * surface friction.
 *
 * <p>The collision shape is a list of axis-aligned boxes expressed in
 * <em>cell-local</em> coordinates — every component lies in
 * {@code [0, 1]}, where {@code (0, 0, 0)} is the block's minimum
 * corner. A consumer translates a box into world space by adding the
 * block's integer coordinates. A full cube is the single box
 * {@code (0,0,0)-(1,1,1)}; a passable block has no boxes.
 *
 * <p>Shapes that extend beyond a single cell (fences, walls, the
 * raised collision of a fence gate) are not modelled — their boxes
 * are clipped to the unit cell. This is a documented baseline.
 *
 * <p>Instances are immutable. The named constants are singletons;
 * {@link #UNKNOWN} in particular is compared by identity to mark a
 * desynced cell distinct from a genuinely passable one.
 */
public final class BlockState {

    /** Cell-local box of a full unit cube. */
    private static final Aabb FULL_CUBE = new Aabb(0, 0, 0, 1, 1, 1);

    /**
     * Not cached. The world is desynced here; checks must decide how
     * to treat the uncertainty. Treated as having no collision.
     */
    public static final BlockState UNKNOWN =
            new BlockState("unknown", List.of(), MotionFormulas.DEFAULT_SLIPPERINESS);

    /** No collision — the player passes through. */
    public static final BlockState PASSABLE =
            new BlockState("passable", List.of(), MotionFormulas.DEFAULT_SLIPPERINESS);

    /** Full unit-cube collision with ordinary friction. */
    public static final BlockState SOLID =
            new BlockState("solid", List.of(FULL_CUBE), MotionFormulas.DEFAULT_SLIPPERINESS);

    /** Full-cube ice: very slippery. */
    public static final BlockState ICE = cube("ice", 0.98);

    /** Full-cube packed ice: very slippery, like {@link #ICE}. */
    public static final BlockState PACKED_ICE = cube("packed_ice", 0.98);

    /** Full-cube blue ice: the slipperiest surface. */
    public static final BlockState BLUE_ICE = cube("blue_ice", 0.989);

    /** Full-cube slime block: slightly slippery. */
    public static final BlockState SLIME_BLOCK = cube("slime_block", 0.8);

    /** A slab occupying the lower half of its cell. */
    public static final BlockState BOTTOM_SLAB = new BlockState("bottom_slab",
            List.of(new Aabb(0, 0, 0, 1, 0.5, 1)), MotionFormulas.DEFAULT_SLIPPERINESS);

    /** A slab occupying the upper half of its cell. */
    public static final BlockState TOP_SLAB = new BlockState("top_slab",
            List.of(new Aabb(0, 0.5, 0, 1, 1, 1)), MotionFormulas.DEFAULT_SLIPPERINESS);

    private final String name;
    private final List<Aabb> collisionBoxes;
    private final double slipperiness;

    private BlockState(String name, List<Aabb> collisionBoxes, double slipperiness) {
        this.name = name;
        this.collisionBoxes = collisionBoxes;
        this.slipperiness = slipperiness;
    }

    /** A full-cube block with the given name and surface slipperiness. */
    public static BlockState cube(String name, double slipperiness) {
        return new BlockState(Objects.requireNonNull(name, "name"),
                List.of(FULL_CUBE), requireSlipperiness(slipperiness));
    }

    /**
     * A block with an arbitrary collision shape. Every box must be
     * expressed in cell-local coordinates (components in
     * {@code [0, 1]}). Passing no boxes yields a passable block.
     */
    public static BlockState shape(String name, double slipperiness, Aabb... boxes) {
        Objects.requireNonNull(name, "name");
        for (Aabb box : boxes) {
            requireCellLocal(box);
        }
        return new BlockState(name, List.of(boxes), requireSlipperiness(slipperiness));
    }

    /** Identifier of this block kind, for logging and debugging. */
    public String name() {
        return name;
    }

    /**
     * The collision shape, as cell-local boxes. Empty when the block
     * has no collision. Never {@code null}.
     */
    public List<Aabb> collisionBoxes() {
        return collisionBoxes;
    }

    /** Surface friction (slipperiness) of the block's top face. */
    public double slipperiness() {
        return slipperiness;
    }

    /** True when the block has at least one collision box. */
    public boolean hasCollision() {
        return !collisionBoxes.isEmpty();
    }

    /** True only for the {@link #UNKNOWN} sentinel. */
    public boolean isUnknown() {
        return this == UNKNOWN;
    }

    /** True when the block is cached and has no collision. */
    public boolean isPassable() {
        return this != UNKNOWN && collisionBoxes.isEmpty();
    }

    private static double requireSlipperiness(double slipperiness) {
        if (slipperiness <= 0.0 || slipperiness > 1.0) {
            throw new IllegalArgumentException("slipperiness must be in (0, 1]");
        }
        return slipperiness;
    }

    private static void requireCellLocal(Aabb box) {
        if (box.minX() < 0 || box.minY() < 0 || box.minZ() < 0
                || box.maxX() > 1 || box.maxY() > 1 || box.maxZ() > 1) {
            throw new IllegalArgumentException(
                    "collision box must be cell-local (components in [0, 1]): " + box);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof BlockState state
                && name.equals(state.name)
                && collisionBoxes.equals(state.collisionBoxes)
                && Double.compare(slipperiness, state.slipperiness) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, collisionBoxes, slipperiness);
    }

    @Override
    public String toString() {
        return "BlockState[" + name + ']';
    }
}
