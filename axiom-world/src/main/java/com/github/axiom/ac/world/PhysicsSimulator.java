package com.github.axiom.ac.world;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.MotionFormulas;
import com.github.axiom.ac.math.Vec3;
import java.util.Objects;

/**
 * Predicts a player's motion over one tick, resolving collisions
 * against the cached world.
 *
 * <p>Vertical velocity decays under gravity and drag; horizontal
 * velocity decays under ground friction. The resulting motion is
 * then resolved axis by axis (Y, X, Z): a component that would push
 * the player's bounding box into a solid block is cancelled.
 *
 * <p>Horizontal friction currently assumes an ordinary (non-ice)
 * block; ice and slime surfaces are not yet modelled.
 */
public final class PhysicsSimulator {

    private final CollisionEngine collision;

    public PhysicsSimulator(CollisionEngine collision) {
        this.collision = Objects.requireNonNull(collision, "collision");
    }

    /**
     * Result of one simulated tick.
     *
     * @param box      the player bounding box after the tick
     * @param velocity the velocity after collision resolution
     * @param onGround whether the player ended supported from below
     */
    public record Result(Aabb box, Vec3 velocity, boolean onGround) {
    }

    /**
     * Simulates one tick for a player whose bounding box is
     * {@code box}, current velocity is {@code velocity}, and
     * client-reported ground state is {@code onGround}.
     */
    public Result simulate(Aabb box, Vec3 velocity, boolean onGround) {
        double friction = MotionFormulas.horizontalFriction(MotionFormulas.DEFAULT_SLIPPERINESS);
        double wantX = MotionFormulas.nextHorizontalVelocity(velocity.x(), friction);
        double wantY = MotionFormulas.nextVerticalVelocity(velocity.y());
        double wantZ = MotionFormulas.nextHorizontalVelocity(velocity.z(), friction);

        // Resolve axis by axis: a component that would drive the
        // bounding box into a solid block is cancelled.
        double resolvedY = collision.collides(box.offset(0.0, wantY, 0.0)) ? 0.0 : wantY;
        Aabb afterY = box.offset(0.0, resolvedY, 0.0);

        double resolvedX = collision.collides(afterY.offset(wantX, 0.0, 0.0)) ? 0.0 : wantX;
        Aabb afterX = afterY.offset(resolvedX, 0.0, 0.0);

        double resolvedZ = collision.collides(afterX.offset(0.0, 0.0, wantZ)) ? 0.0 : wantZ;
        Aabb afterZ = afterX.offset(0.0, 0.0, resolvedZ);

        // A descending player whose downward motion was cancelled is
        // supported from below. Gravity guarantees wantY < 0 for any
        // player that is not actively rising.
        boolean supported = wantY < 0.0 && resolvedY == 0.0;
        Vec3 resolvedVelocity = new Vec3(resolvedX, resolvedY, resolvedZ);
        return new Result(afterZ, resolvedVelocity, supported);
    }
}
