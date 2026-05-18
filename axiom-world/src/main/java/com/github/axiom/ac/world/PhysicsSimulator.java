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

        double resolvedY = resolveAxis(box, 0.0, wantY, 0.0);
        Aabb afterY = box.offset(0.0, resolvedY, 0.0);

        double resolvedX = resolveAxis(afterY, wantX, 0.0, 0.0);
        Aabb afterX = afterY.offset(resolvedX, 0.0, 0.0);

        double resolvedZ = resolveAxis(afterX, 0.0, 0.0, wantZ);
        Aabb afterZ = afterX.offset(0.0, 0.0, resolvedZ);

        boolean supported = wantY <= 0.0 && resolvedY == 0.0;
        Vec3 resolvedVelocity = new Vec3(resolvedX, resolvedY, resolvedZ);
        return new Result(afterZ, resolvedVelocity, supported);
    }

    /**
     * Returns the requested single-axis movement, or 0 when applying
     * it would drive {@code box} into a solid block.
     */
    private double resolveAxis(Aabb box, double dx, double dy, double dz) {
        double requested = dx + dy + dz;
        if (requested == 0.0) {
            return 0.0;
        }
        return collision.collides(box.offset(dx, dy, dz)) ? 0.0 : requested;
    }
}
