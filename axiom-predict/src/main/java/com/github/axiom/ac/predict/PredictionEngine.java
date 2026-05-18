package com.github.axiom.ac.predict;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.MotionFormulas;
import com.github.axiom.ac.math.Vec3;
import com.github.axiom.ac.world.BlockPos;
import com.github.axiom.ac.world.BlockState;
import com.github.axiom.ac.world.PhysicsSimulator;
import com.github.axiom.ac.world.WorldCache;
import java.util.Objects;

/**
 * Predicts the next {@link PlayerState} for a candidate
 * {@link MovementInput}. Input axes become a world-space horizontal
 * acceleration via Minecraft's {@code moveRelative} direction math;
 * a jump from the ground replaces the vertical velocity with the
 * jump impulse; and the collision-aware tick is delegated to
 * {@link PhysicsSimulator}.
 *
 * <p>The input acceleration follows Minecraft's {@code travel} model:
 * on the ground it scales with the inverse cube of the surface
 * friction (so movement on ice accelerates slowly but coasts far);
 * in the air it is a small fixed value. Surface slipperiness is read
 * from the block beneath the player.
 *
 * <p>The movement constants are the documented 1.21 baseline. The
 * engine's tick ordering is exact; the constants are a close
 * approximation rather than a per-version-tuned table.
 */
public final class PredictionEngine {

    /** Width of the standard player bounding box. */
    public static final double PLAYER_WIDTH = 0.6;

    /** Height of the standard player bounding box. */
    public static final double PLAYER_HEIGHT = 1.8;

    private static final double INPUT_EPSILON = 1.0e-4;
    private static final double GROUND_PROBE_EPSILON = 1.0e-3;

    private final PhysicsSimulator simulator;

    public PredictionEngine(PhysicsSimulator simulator) {
        this.simulator = Objects.requireNonNull(simulator, "simulator");
    }

    /**
     * Predicts the state one tick after {@code previous}, assuming
     * the client pressed {@code input}.
     */
    public PlayerState predict(PlayerState previous, MovementInput input) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(input, "input");

        boolean onGround = previous.onGround();
        double slipperiness = slipperinessBelow(previous);
        double frictionXz = onGround
                ? MotionFormulas.horizontalFriction(slipperiness)
                : MotionFormulas.AIR_FRICTION;

        Vec3 acceleration = inputAcceleration(previous.yaw(), input, onGround, frictionXz);

        Vec3 velocity = previous.velocity();
        if (input.jump() && onGround) {
            velocity = new Vec3(velocity.x(), MotionFormulas.jumpVelocity(0), velocity.z());
        }

        Aabb box = boxAt(previous.position());
        PhysicsSimulator.Result result =
                simulator.simulate(box, velocity, acceleration, onGround, slipperiness);
        return new PlayerState(feetOf(result.box()), result.velocity(),
                previous.yaw(), result.onGround());
    }

    /**
     * World-space horizontal acceleration produced by {@code input}
     * for a player looking along {@code yaw}. Uses Minecraft's
     * {@code moveRelative}: the input vector is normalised against its
     * own magnitude (only when that exceeds one), scaled by the
     * per-tick move acceleration, and rotated by the yaw.
     */
    private static Vec3 inputAcceleration(float yaw, MovementInput input,
                                          boolean onGround, double frictionXz) {
        double strafe = input.strafe();
        double forward = input.forward();
        double magnitude = Math.sqrt(strafe * strafe + forward * forward);
        if (magnitude < INPUT_EPSILON) {
            return Vec3.ZERO;
        }
        double acceleration = onGround
                ? MotionFormulas.groundAcceleration(frictionXz, input.sprint())
                : MotionFormulas.airAcceleration(input.sprint());
        double scale = acceleration / Math.max(magnitude, 1.0);
        strafe *= scale;
        forward *= scale;

        double yawRad = Math.toRadians(yaw);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double accelX = strafe * cos - forward * sin;
        double accelZ = forward * cos + strafe * sin;
        return new Vec3(accelX, 0.0, accelZ);
    }

    /**
     * Slipperiness of the block beneath the player's feet, or the
     * default when the player is airborne or that block is uncached.
     */
    private double slipperinessBelow(PlayerState state) {
        if (!state.onGround()) {
            return MotionFormulas.DEFAULT_SLIPPERINESS;
        }
        Vec3 feet = state.position();
        BlockPos below = new BlockPos(
                (int) Math.floor(feet.x()),
                (int) Math.floor(feet.y() - GROUND_PROBE_EPSILON),
                (int) Math.floor(feet.z()));
        BlockState block = simulator.collision().world().blockAt(below);
        return block.hasCollision()
                ? block.slipperiness()
                : MotionFormulas.DEFAULT_SLIPPERINESS;
    }

    /** The standard player bounding box around a feet position. */
    static Aabb boxAt(Vec3 feet) {
        double half = PLAYER_WIDTH / 2.0;
        return new Aabb(
                feet.x() - half, feet.y(), feet.z() - half,
                feet.x() + half, feet.y() + PLAYER_HEIGHT, feet.z() + half);
    }

    /** The feet position (bottom centre) of a player bounding box. */
    static Vec3 feetOf(Aabb box) {
        return new Vec3(
                (box.minX() + box.maxX()) / 2.0,
                box.minY(),
                (box.minZ() + box.maxZ()) / 2.0);
    }
}
