package com.github.axiom.ac.world;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.MotionFormulas;
import com.github.axiom.ac.math.Vec3;
import java.util.Objects;

/**
 * Predicts a player's motion over one tick, resolving collisions
 * against the cached world.
 *
 * <p>One simulated tick, in Minecraft's order:
 * <ol>
 *   <li>the previous velocity decays — horizontally under friction,
 *       vertically under gravity and drag;</li>
 *   <li>the caller's input acceleration is added to the decayed
 *       horizontal velocity;</li>
 *   <li>the resulting motion is resolved axis by axis (Y, X, Z): a
 *       component that would push the player's bounding box into a
 *       block's collision shape is cancelled.</li>
 * </ol>
 *
 * <p>Decaying <em>before</em> applying input is what keeps input
 * acceleration from being multiplied by friction in the same tick.
 *
 * <p>Horizontal friction depends on the surface: a grounded player
 * decays under {@code slipperiness * 0.91}, an airborne one under the
 * bare air friction {@code 0.91}. When the slipperiness is not given
 * explicitly it is read from the block beneath the bounding box.
 */
public final class PhysicsSimulator {

    private final CollisionEngine collision;

    public PhysicsSimulator(CollisionEngine collision) {
        this.collision = Objects.requireNonNull(collision, "collision");
    }

    /** The collision engine, and through it the world, this simulator uses. */
    public CollisionEngine collision() {
        return collision;
    }

    /**
     * Result of one simulated tick.
     *
     * @param box      the player bounding box after the tick
     * @param velocity the velocity after collision resolution — the
     *                 velocity to feed into the next tick
     * @param onGround whether the player ended supported from below
     */
    public record Result(Aabb box, Vec3 velocity, boolean onGround) {
    }

    /**
     * Simulates one tick with no input acceleration, reading the
     * surface slipperiness from the block beneath {@code box}.
     */
    public Result simulate(Aabb box, Vec3 velocity, boolean onGround) {
        return simulate(box, velocity, Vec3.ZERO, onGround);
    }

    /**
     * Simulates one tick with the given horizontal input
     * acceleration, reading the surface slipperiness from the block
     * beneath {@code box}.
     */
    public Result simulate(Aabb box, Vec3 velocity, Vec3 inputAcceleration,
                           boolean onGround) {
        return simulate(box, velocity, inputAcceleration, onGround,
                slipperinessBelow(box));
    }

    /**
     * Simulates one tick for a player whose bounding box is
     * {@code box} and current velocity is {@code velocity}, adding
     * {@code inputAcceleration} after the friction decay.
     *
     * @param box               the player bounding box
     * @param velocity          velocity carried from the previous tick
     * @param inputAcceleration world-space acceleration from movement
     *                          input this tick (typically horizontal)
     * @param onGround          client-reported ground state, which
     *                          selects ground or air friction
     * @param slipperiness      slipperiness of the supporting surface
     */
    public Result simulate(Aabb box, Vec3 velocity, Vec3 inputAcceleration,
                           boolean onGround, double slipperiness) {
        double frictionXz = onGround
                ? MotionFormulas.horizontalFriction(slipperiness)
                : MotionFormulas.AIR_FRICTION;

        double wantX = velocity.x() * frictionXz + inputAcceleration.x();
        double wantY = MotionFormulas.nextVerticalVelocity(velocity.y())
                + inputAcceleration.y();
        double wantZ = velocity.z() * frictionXz + inputAcceleration.z();

        // Resolve axis by axis: a component that would drive the
        // bounding box into a block's collision shape is cancelled.
        double resolvedY = collision.collides(box.offset(0.0, wantY, 0.0)) ? 0.0 : wantY;
        Aabb afterY = box.offset(0.0, resolvedY, 0.0);

        double resolvedX = collision.collides(afterY.offset(wantX, 0.0, 0.0)) ? 0.0 : wantX;
        Aabb afterX = afterY.offset(resolvedX, 0.0, 0.0);

        double resolvedZ = collision.collides(afterX.offset(0.0, 0.0, wantZ)) ? 0.0 : wantZ;
        Aabb afterZ = afterX.offset(0.0, 0.0, resolvedZ);

        // A descending player whose downward motion was cancelled is
        // supported from below.
        boolean supported = wantY < 0.0 && resolvedY == 0.0;
        Vec3 resolvedVelocity = new Vec3(resolvedX, resolvedY, resolvedZ);
        return new Result(afterZ, resolvedVelocity, supported);
    }

    /**
     * Slipperiness of the block directly beneath {@code box}. A small
     * epsilon below the box's base selects the supporting block even
     * when the box rests on a block whose top is below a cell
     * boundary (a slab). Falls back to the default when that block
     * has no collision or is uncached.
     */
    private double slipperinessBelow(Aabb box) {
        double centreX = (box.minX() + box.maxX()) / 2.0;
        double centreZ = (box.minZ() + box.maxZ()) / 2.0;
        BlockPos below = new BlockPos(
                (int) Math.floor(centreX),
                (int) Math.floor(box.minY() - 1.0e-3),
                (int) Math.floor(centreZ));
        BlockState state = collision.world().blockAt(below);
        return state.hasCollision()
                ? state.slipperiness()
                : MotionFormulas.DEFAULT_SLIPPERINESS;
    }
}
