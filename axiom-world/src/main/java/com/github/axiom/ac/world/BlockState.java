package com.github.axiom.ac.world;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.MotionFormulas;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Collision state and material traits of a cached block: its
 * collision shape, its surface friction, and the environmental
 * properties (fluid, climbable, bouncy, cobweb, slowing) that change
 * how a player moves through it.
 *
 * <p>The collision shape is a list of <em>cell-local</em> boxes —
 * every component lies in {@code [0, 1]}, where {@code (0, 0, 0)} is
 * the block's minimum corner. A consumer translates a box into world
 * space by adding the block's integer coordinates. A full cube is the
 * single box {@code (0,0,0)-(1,1,1)}; a passable block has no boxes.
 *
 * <p>Instances are immutable; build custom blocks with {@link
 * #builder(String)}. The named constants are singletons, and {@link
 * #UNKNOWN} is compared by identity to mark a desynced cell distinct
 * from a genuinely passable one.
 */
public final class BlockState {

    /** Cell-local box of a full unit cube. */
    private static final Aabb FULL_CUBE = new Aabb(0, 0, 0, 1, 1, 1);

    /**
     * Not cached. The world is desynced here; checks must decide how
     * to treat the uncertainty. Treated as having no collision.
     */
    public static final BlockState UNKNOWN = builder("unknown").build();

    /** No collision — the player passes through. */
    public static final BlockState PASSABLE = builder("passable").build();

    /** Full unit-cube collision with ordinary friction. */
    public static final BlockState SOLID = builder("solid").fullCube().build();

    /** Full-cube ice: very slippery. */
    public static final BlockState ICE = builder("ice").fullCube().slipperiness(0.98).build();

    /** Full-cube packed ice: very slippery, like {@link #ICE}. */
    public static final BlockState PACKED_ICE =
            builder("packed_ice").fullCube().slipperiness(0.98).build();

    /** Full-cube blue ice: the slipperiest surface. */
    public static final BlockState BLUE_ICE =
            builder("blue_ice").fullCube().slipperiness(0.989).build();

    /** Full-cube slime block: slightly slippery and bouncy. */
    public static final BlockState SLIME_BLOCK =
            builder("slime_block").fullCube().slipperiness(0.8).bouncy(true).build();

    /** Full-cube soul sand: ordinary collision, but slows movement. */
    public static final BlockState SOUL_SAND =
            builder("soul_sand").fullCube().speedMultiplier(0.4).build();

    /** Full-cube honey block: slows movement and limits jumps. */
    public static final BlockState HONEY_BLOCK =
            builder("honey_block").fullCube().speedMultiplier(0.4).build();

    /** A slab occupying the lower half of its cell. */
    public static final BlockState BOTTOM_SLAB =
            builder("bottom_slab").shape(new Aabb(0, 0, 0, 1, 0.5, 1)).build();

    /** A slab occupying the upper half of its cell. */
    public static final BlockState TOP_SLAB =
            builder("top_slab").shape(new Aabb(0, 0.5, 0, 1, 1, 1)).build();

    /** A water source or flow: passable, but applies fluid physics. */
    public static final BlockState WATER = builder("water").fluid(Fluid.WATER).build();

    /** A lava source or flow: passable, but applies fluid physics. */
    public static final BlockState LAVA = builder("lava").fluid(Fluid.LAVA).build();

    /** A ladder: passable, but climbable. */
    public static final BlockState LADDER = builder("ladder").climbable(true).build();

    /** A vine: passable, but climbable. */
    public static final BlockState VINE = builder("vine").climbable(true).build();

    /** Scaffolding: passable, climbable, and descended through on sneak. */
    public static final BlockState SCAFFOLDING =
            builder("scaffolding").climbable(true).scaffolding(true).build();

    /** A cobweb: passable, but entangles and drastically slows motion. */
    public static final BlockState COBWEB = builder("cobweb").cobweb(true).build();

    /** Water carrying an upward bubble column (above soul sand). */
    public static final BlockState BUBBLE_COLUMN_UPWARD = builder("bubble_column_up")
            .fluid(Fluid.WATER).bubbleColumn(BubbleColumn.UPWARD).build();

    /** Water carrying a downward bubble column (above magma). */
    public static final BlockState BUBBLE_COLUMN_DOWNWARD = builder("bubble_column_down")
            .fluid(Fluid.WATER).bubbleColumn(BubbleColumn.DOWNWARD).build();

    /** Powder snow: passable, but a player without leather boots sinks. */
    public static final BlockState POWDER_SNOW =
            builder("powder_snow").powderSnow(true).build();

    private final String name;
    private final List<Aabb> collisionBoxes;
    private final double slipperiness;
    private final Fluid fluid;
    private final boolean climbable;
    private final boolean bouncy;
    private final boolean cobweb;
    private final boolean scaffolding;
    private final boolean powderSnow;
    private final BubbleColumn bubbleColumn;
    private final double speedMultiplier;

    private BlockState(Builder builder) {
        this.name = builder.name;
        this.collisionBoxes = ShapeNormalizer.normalize(builder.boxes);
        this.slipperiness = builder.slipperiness;
        this.fluid = builder.fluid;
        this.climbable = builder.climbable;
        this.bouncy = builder.bouncy;
        this.cobweb = builder.cobweb;
        this.scaffolding = builder.scaffolding;
        this.powderSnow = builder.powderSnow;
        this.bubbleColumn = builder.bubbleColumn;
        this.speedMultiplier = builder.speedMultiplier;
    }

    /** A new builder for a block kind named {@code name}. */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /** A full-cube block with the given name and surface slipperiness. */
    public static BlockState cube(String name, double slipperiness) {
        return builder(name).fullCube().slipperiness(slipperiness).build();
    }

    /**
     * A block with an arbitrary collision shape. Every box must be
     * expressed in cell-local coordinates (components in
     * {@code [0, 1]}). Passing no boxes yields a passable block.
     */
    public static BlockState shape(String name, double slipperiness, Aabb... boxes) {
        return builder(name).slipperiness(slipperiness).shape(boxes).build();
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

    /** The fluid this block contributes, or {@link Fluid#NONE}. */
    public Fluid fluid() {
        return fluid;
    }

    /** True when a player whose hitbox overlaps this block can climb it. */
    public boolean isClimbable() {
        return climbable;
    }

    /** True when this block bounces a player who lands on it (slime). */
    public boolean isBouncy() {
        return bouncy;
    }

    /** True when this block entangles a player (cobweb). */
    public boolean isCobweb() {
        return cobweb;
    }

    /** True when this block is scaffolding, descended through on sneak. */
    public boolean isScaffolding() {
        return scaffolding;
    }

    /** True when this block is powder snow, sunk through without boots. */
    public boolean isPowderSnow() {
        return powderSnow;
    }

    /** The bubble column this block carries, or {@link BubbleColumn#NONE}. */
    public BubbleColumn bubbleColumn() {
        return bubbleColumn;
    }

    /**
     * Horizontal movement multiplier applied while standing on this
     * block; {@code 1.0} for an ordinary surface, lower for soul sand
     * and honey.
     */
    public double speedMultiplier() {
        return speedMultiplier;
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

    /** True when this block carries a fluid. */
    public boolean isFluid() {
        return fluid != Fluid.NONE;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof BlockState state
                && name.equals(state.name)
                && collisionBoxes.equals(state.collisionBoxes)
                && Double.compare(slipperiness, state.slipperiness) == 0
                && fluid == state.fluid
                && climbable == state.climbable
                && bouncy == state.bouncy
                && cobweb == state.cobweb
                && scaffolding == state.scaffolding
                && powderSnow == state.powderSnow
                && bubbleColumn == state.bubbleColumn
                && Double.compare(speedMultiplier, state.speedMultiplier) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, collisionBoxes, slipperiness, fluid,
                climbable, bouncy, cobweb, scaffolding, powderSnow,
                bubbleColumn, speedMultiplier);
    }

    @Override
    public String toString() {
        return "BlockState[" + name + ']';
    }

    /** Fluent builder for a {@link BlockState}. */
    public static final class Builder {

        private final String name;
        private final List<Aabb> boxes = new ArrayList<>();
        private double slipperiness = MotionFormulas.DEFAULT_SLIPPERINESS;
        private Fluid fluid = Fluid.NONE;
        private boolean climbable;
        private boolean bouncy;
        private boolean cobweb;
        private boolean scaffolding;
        private boolean powderSnow;
        private BubbleColumn bubbleColumn = BubbleColumn.NONE;
        private double speedMultiplier = 1.0;

        private Builder(String name) {
            this.name = Objects.requireNonNull(name, "name");
        }

        /** Gives the block a full unit-cube collision shape. */
        public Builder fullCube() {
            boxes.clear();
            boxes.add(FULL_CUBE);
            return this;
        }

        /**
         * Sets an arbitrary collision shape. Every box must be
         * cell-local (components in {@code [0, 1]}).
         */
        public Builder shape(Aabb... cellLocalBoxes) {
            boxes.clear();
            for (Aabb box : cellLocalBoxes) {
                requireCellLocal(box);
                boxes.add(box);
            }
            return this;
        }

        /** Sets the surface slipperiness; must be in {@code (0, 1]}. */
        public Builder slipperiness(double slipperiness) {
            if (slipperiness <= 0.0 || slipperiness > 1.0) {
                throw new IllegalArgumentException("slipperiness must be in (0, 1]");
            }
            this.slipperiness = slipperiness;
            return this;
        }

        /** Sets the fluid this block contributes. */
        public Builder fluid(Fluid fluid) {
            this.fluid = Objects.requireNonNull(fluid, "fluid");
            return this;
        }

        /** Marks the block climbable (ladder, vine, scaffolding). */
        public Builder climbable(boolean climbable) {
            this.climbable = climbable;
            return this;
        }

        /** Marks the block bouncy (slime). */
        public Builder bouncy(boolean bouncy) {
            this.bouncy = bouncy;
            return this;
        }

        /** Marks the block a cobweb. */
        public Builder cobweb(boolean cobweb) {
            this.cobweb = cobweb;
            return this;
        }

        /** Marks the block scaffolding (climbable, sneak-descendable). */
        public Builder scaffolding(boolean scaffolding) {
            this.scaffolding = scaffolding;
            return this;
        }

        /** Marks the block powder snow (a player without boots sinks). */
        public Builder powderSnow(boolean powderSnow) {
            this.powderSnow = powderSnow;
            return this;
        }

        /** Sets the bubble column this block carries. */
        public Builder bubbleColumn(BubbleColumn bubbleColumn) {
            this.bubbleColumn = Objects.requireNonNull(bubbleColumn, "bubbleColumn");
            return this;
        }

        /** Sets the horizontal movement multiplier; must be positive. */
        public Builder speedMultiplier(double speedMultiplier) {
            if (speedMultiplier <= 0.0) {
                throw new IllegalArgumentException("speedMultiplier must be positive");
            }
            this.speedMultiplier = speedMultiplier;
            return this;
        }

        /**
         * Builds the immutable {@link BlockState}. The collision shape
         * is canonicalised through {@link ShapeNormalizer}: degenerate,
         * duplicate, and enclosed boxes are dropped and the rest sorted.
         */
        public BlockState build() {
            return new BlockState(this);
        }

        private static void requireCellLocal(Aabb box) {
            if (box.minX() < 0 || box.minY() < 0 || box.minZ() < 0
                    || box.maxX() > 1 || box.maxY() > 1 || box.maxZ() > 1) {
                throw new IllegalArgumentException(
                        "collision box must be cell-local (components in [0, 1]): " + box);
            }
        }
    }
}
