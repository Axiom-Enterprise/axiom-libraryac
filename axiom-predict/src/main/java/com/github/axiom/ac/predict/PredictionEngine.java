package com.github.axiom.ac.predict;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.MotionFormulas;
import com.github.axiom.ac.math.Vec3;
import com.github.axiom.ac.world.PhysicsSimulator;
import java.util.Objects;

/**
 * Predicts the next {@link PlayerState} for a candidate
 * {@link MovementInput}. Input axes become a world-space acceleration
 * via Minecraft's {@code moveRelative} direction math; the result is
 * added to the velocity, a jump impulse is applied when jumping from
 * the ground, and the collision-aware tick is delegated to
 * {@link PhysicsSimulator}.
 *
 * <p>The movement constants are a baseline approximation — see the
 * plan's "Physics-accuracy scope" note. The engine is exact; the
 * constants are not yet tuned to a specific Minecraft version.
 */
public final class PredictionEngine {

    /** Width of the standard player bounding box. */
    public static final double PLAYER_WIDTH = 0.6;

    /** Height of the standard player bounding box. */
    public static final double PLAYER_HEIGHT = 1.8;

    /** Baseline per-tick horizontal acceleration for walking. */
    public static final double BASE_MOVE_SPEED = 0.1;

    /** Multiplier applied to the move speed while sprinting. */
    public static final double SPRINT_MULTIPLIER = 1.3;

    private static final double INPUT_EPSILON = 1.0e-4;

    private final PhysicsSimulator simulator;

    public PredictionEngine(PhysicsSimulator simulator) {
        this.simulator = Objects.requireNonNull(simulator, "simulator");
    }

    /**
     * Predicts the state one tick after {@code previous}, assuming
     * the client pressed {@code input}.
     */
    public PlayerState predict(PlayerState previous, MovementInput input) {
        Vec3 acceleration = inputAcceleration(previous.yaw(), input);

        Vec3 velocity = previous.velocity();
        double velocityY = velocity.y();
        if (input.jump() && previous.onGround()) {
            velocityY = MotionFormulas.jumpVelocity(0);
        }
        Vec3 inputVelocity = new Vec3(
                velocity.x() + acceleration.x(),
                velocityY,
                velocity.z() + acceleration.z());

        Aabb box = boxAt(previous.position());
        PhysicsSimulator.Result result =
                simulator.simulate(box, inputVelocity, previous.onGround());
        return new PlayerState(feetOf(result.box()), result.velocity(),
                previous.yaw(), result.onGround());
    }

    /**
     * World-space horizontal acceleration produced by {@code input}
     * for a player looking along {@code yaw}. Uses Minecraft's
     * {@code moveRelative}: the input vector is normalised, scaled by
     * the move speed, and rotated by the yaw.
     */
    private static Vec3 inputAcceleration(float yaw, MovementInput input) {
        double strafe = input.strafe();
        double forward = input.forward();
        double magnitude = Math.sqrt(strafe * strafe + forward * forward);
        if (magnitude < INPUT_EPSILON) {
            return new Vec3(0, 0, 0);
        }
        double speed = BASE_MOVE_SPEED * (input.sprint() ? SPRINT_MULTIPLIER : 1.0);
        double scale = speed / Math.max(magnitude, 1.0);
        strafe *= scale;
        forward *= scale;

        double yawRad = Math.toRadians(yaw);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double accelX = strafe * cos - forward * sin;
        double accelZ = forward * cos + strafe * sin;
        return new Vec3(accelX, 0.0, accelZ);
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
