package com.github.axiom.ac.world;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.MotionFormulas;
import com.github.axiom.ac.math.Vec3;
import java.util.Objects;

/**
 * Resolves a player's motion against the cached world.
 *
 * <p>Two layers are exposed. {@link #move} is the pure collision
 * mover: given a velocity it resolves collisions axis by axis (Y, X,
 * Z) and, optionally, steps the player up small ledges. {@link
 * #simulate} adds the ordinary walking-physics velocity update on top
 * — friction decay, gravity, then input acceleration — and is the
 * convenience used for plain ground and air movement; other media
 * (water, lava, climbing, elytra) compute their own velocity and call
 * {@link #move} directly.
 */
public final class PhysicsSimulator {

    private static final int SETTLE_ITERATIONS = 40;

    private final CollisionEngine collision;

    public PhysicsSimulator(CollisionEngine collision) {
        this.collision = Objects.requireNonNull(collision, "collision");
    }

    /** The collision engine, and through it the world, this simulator uses. */
    public CollisionEngine collision() {
        return collision;
    }

    /**
     * Result of one resolved move.
     *
     * @param box      the player bounding box afterward
     * @param velocity the velocity after collision resolution — the
     *                 velocity to feed into the next tick
     * @param onGround whether the player ended supported from below
     */
    public record Result(Aabb box, Vec3 velocity, boolean onGround) {
    }

    // ---- collision mover -------------------------------------------------

    /**
     * Moves {@code box} by {@code velocity}, resolving collisions
     * axis by axis. A component that would drive the box into a
     * block's collision shape is cancelled. When {@code stepAssist}
     * is set and the player is grounded, a horizontal move blocked by
     * a ledge no taller than {@link MotionFormulas#STEP_HEIGHT} is
     * retried over the top of the obstacle.
     */
    public Result move(Aabb box, Vec3 velocity, boolean onGround, boolean stepAssist) {
        double wantX = velocity.x();
        double wantY = velocity.y();
        double wantZ = velocity.z();

        double resolvedY = collision.collides(box.offset(0.0, wantY, 0.0)) ? 0.0 : wantY;
        Aabb afterY = box.offset(0.0, resolvedY, 0.0);

        double resolvedX = collision.collides(afterY.offset(wantX, 0.0, 0.0)) ? 0.0 : wantX;
        Aabb afterX = afterY.offset(resolvedX, 0.0, 0.0);

        double resolvedZ = collision.collides(afterX.offset(0.0, 0.0, wantZ)) ? 0.0 : wantZ;
        Aabb afterZ = afterX.offset(0.0, 0.0, resolvedZ);

        boolean blocked = (resolvedX != wantX) || (resolvedZ != wantZ);
        if (stepAssist && onGround && blocked) {
            Aabb stepped = tryStep(afterY, wantX, wantZ);
            if (stepped != null && horizontalSpan(stepped, afterY)
                    > horizontalSpan(afterZ, afterY)) {
                boolean supported = wantY <= 0.0;
                return new Result(stepped, new Vec3(wantX, resolvedY, wantZ), supported);
            }
        }

        boolean supported = wantY < 0.0 && resolvedY == 0.0;
        return new Result(afterZ, new Vec3(resolvedX, resolvedY, resolvedZ), supported);
    }

    /**
     * Attempts to step up over an obstacle: lift the box by the step
     * height, carry it through the full horizontal motion, and settle
     * it back down onto whatever it now stands on. Returns the
     * stepped box, or {@code null} when there is no headroom or the
     * step makes no horizontal progress.
     */
    private Aabb tryStep(Aabb afterY, double wantX, double wantZ) {
        double step = MotionFormulas.STEP_HEIGHT;
        if (collision.collides(afterY.offset(0.0, step, 0.0))) {
            return null;
        }
        Aabb raised = afterY.offset(0.0, step, 0.0);

        double rx = collision.collides(raised.offset(wantX, 0.0, 0.0)) ? 0.0 : wantX;
        Aabb raisedX = raised.offset(rx, 0.0, 0.0);
        double rz = collision.collides(raisedX.offset(0.0, 0.0, wantZ)) ? 0.0 : wantZ;
        Aabb raisedXz = raisedX.offset(0.0, 0.0, rz);
        if (rx == 0.0 && rz == 0.0) {
            return null;
        }
        double drop = settleDrop(raisedXz, step);
        return raisedXz.offset(0.0, -drop, 0.0);
    }

    /**
     * Largest distance, up to {@code maxDrop}, that {@code box} can
     * fall without colliding — the gap to the surface beneath it.
     */
    private double settleDrop(Aabb box, double maxDrop) {
        if (!collision.collides(box.offset(0.0, -maxDrop, 0.0))) {
            return maxDrop;
        }
        double clear = 0.0;
        double blockedAt = maxDrop;
        for (int i = 0; i < SETTLE_ITERATIONS; i++) {
            double mid = (clear + blockedAt) / 2.0;
            if (collision.collides(box.offset(0.0, -mid, 0.0))) {
                blockedAt = mid;
            } else {
                clear = mid;
            }
        }
        return clear;
    }

    private static double horizontalSpan(Aabb moved, Aabb from) {
        double dx = moved.minX() - from.minX();
        double dz = moved.minZ() - from.minZ();
        return dx * dx + dz * dz;
    }

    // ---- walking physics -------------------------------------------------

    /**
     * Simulates one tick of ordinary walking physics with no input
     * acceleration, reading the surface slipperiness from the block
     * beneath {@code box}.
     */
    public Result simulate(Aabb box, Vec3 velocity, boolean onGround) {
        return simulate(box, velocity, Vec3.ZERO, onGround);
    }

    /**
     * Simulates one tick of ordinary walking physics with the given
     * horizontal input acceleration, reading the surface slipperiness
     * from the block beneath {@code box}.
     */
    public Result simulate(Aabb box, Vec3 velocity, Vec3 inputAcceleration,
                           boolean onGround) {
        return simulate(box, velocity, inputAcceleration, onGround,
                slipperinessBelow(box));
    }

    /**
     * Simulates one tick of ordinary walking physics: the previous
     * velocity decays (horizontally under friction, vertically under
     * gravity and drag), {@code inputAcceleration} is added after the
     * decay, and the result is resolved against the world with step
     * assistance.
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
        return move(box, new Vec3(wantX, wantY, wantZ), onGround, true);
    }

    /**
     * Slipperiness of the block directly beneath {@code box}. A small
     * epsilon below the box's base selects the supporting block even
     * when the box rests on a block whose top is below a cell
     * boundary (a slab). Falls back to the default when that block
     * has no collision or is uncached.
     */
    public double slipperinessBelow(Aabb box) {
        BlockState state = supportingBlock(box);
        return state.hasCollision()
                ? state.slipperiness()
                : MotionFormulas.DEFAULT_SLIPPERINESS;
    }

    /** The block state directly beneath {@code box}. */
    public BlockState supportingBlock(Aabb box) {
        double centreX = (box.minX() + box.maxX()) / 2.0;
        double centreZ = (box.minZ() + box.maxZ()) / 2.0;
        BlockPos below = new BlockPos(
                (int) Math.floor(centreX),
                (int) Math.floor(box.minY() - 1.0e-3),
                (int) Math.floor(centreZ));
        return collision.world().blockAt(below);
    }
}
