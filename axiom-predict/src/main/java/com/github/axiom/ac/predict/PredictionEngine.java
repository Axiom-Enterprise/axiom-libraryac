package com.github.axiom.ac.predict;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.MotionFormulas;
import com.github.axiom.ac.math.Vec3;
import com.github.axiom.ac.world.BlockPos;
import com.github.axiom.ac.world.BlockState;
import com.github.axiom.ac.world.BubbleColumn;
import com.github.axiom.ac.world.Fluid;
import com.github.axiom.ac.world.PhysicsSimulator;
import com.github.axiom.ac.world.WorldCache;
import java.util.Objects;

/**
 * Predicts the next {@link PlayerState} for a candidate
 * {@link MovementInput} and {@link MovementContext}.
 *
 * <p>The engine reproduces Minecraft's branched movement model. Each
 * tick it scans the blocks the player overlaps and selects a medium:
 *
 * <ul>
 *   <li><b>elytra</b> — gliding flight, lift and drag from the look
 *       vector, optionally boosted by a firework rocket;</li>
 *   <li><b>water / lava</b> — fluid drag, buoyant sink, swim input,
 *       Depth Strider, Dolphin's Grace, and bubble columns;</li>
 *   <li><b>powder snow</b> — a bootless player sinks slowly;</li>
 *   <li><b>climbable</b> — ladders, vines, and scaffolding clamp and
 *       drive vertical motion;</li>
 *   <li><b>walking</b> — the ordinary ground and air model: friction
 *       decay, then {@code moveRelative} input acceleration scaled by
 *       the inverse cube of the surface friction, then collision with
 *       step assistance.</li>
 * </ul>
 *
 * <p>Across every branch it also applies sprint rules (sprint needs
 * forward), sneak slowdown, Jump Boost, sprint-jump impulse, the
 * Speed and Slowness effects, Levitation, Slow Falling, soul-sand and
 * honey slowdown, slime bounce, and cobweb entanglement.
 *
 * <p>The tick ordering and branch selection are exact; the movement
 * constants are a documented 1.21 baseline approximation.
 */
public final class PredictionEngine {

    /** Width of the standard player bounding box. */
    public static final double PLAYER_WIDTH = 0.6;

    /** Height of the standard player bounding box. */
    public static final double PLAYER_HEIGHT = 1.8;

    /** Horizontal movement multiplier applied while sneaking. */
    public static final double SNEAK_MULTIPLIER = 0.3;

    /** Forward velocity impulse added by a sprint jump. */
    public static final double SPRINT_JUMP_IMPULSE = 0.2;

    /** Upward velocity added per tick by holding jump while in a fluid. */
    public static final double SWIM_UP_IMPULSE = 0.04;

    private static final double INPUT_EPSILON = 1.0e-4;

    private final PhysicsSimulator simulator;

    public PredictionEngine(PhysicsSimulator simulator) {
        this.simulator = Objects.requireNonNull(simulator, "simulator");
    }

    /**
     * Predicts the state one tick after {@code previous} with no
     * active effects or elytra.
     */
    public PlayerState predict(PlayerState previous, MovementInput input) {
        return predict(previous, input, MovementContext.none());
    }

    /**
     * Predicts the state one tick after {@code previous}, assuming
     * the client pressed {@code input} under {@code context}.
     */
    public PlayerState predict(PlayerState previous, MovementInput input,
                               MovementContext context) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(context, "context");

        Aabb box = boxAt(previous.position());
        Environment env = scan(box);

        Vec3 velocity = previous.velocity();
        if (env.cobweb()) {
            velocity = new Vec3(
                    velocity.x() * MotionFormulas.COBWEB_DRAG_HORIZONTAL,
                    velocity.y() * MotionFormulas.COBWEB_DRAG_VERTICAL,
                    velocity.z() * MotionFormulas.COBWEB_DRAG_HORIZONTAL);
        }

        Vec3 look = previous.rotation().directionVector();
        if (context.hasRiptideLaunch()) {
            velocity = velocity.add(look.scale(
                    MotionFormulas.riptideLaunchSpeed(context.riptideLevel())));
        }

        boolean gliding = context.elytra() && !previous.onGround()
                && !env.water() && !env.lava();
        if (gliding && context.fireworkBoost()) {
            velocity = applyFireworkBoost(velocity, look);
        }

        PhysicsSimulator.Result result;
        if (gliding) {
            result = elytraTick(previous, box, velocity);
        } else if (env.water()) {
            result = fluidTick(previous, input, box, velocity, env, context);
        } else if (env.lava()) {
            result = fluidTick(previous, input, box, velocity, env, context);
        } else if (env.powderSnow()) {
            result = powderSnowTick(previous, input, box, velocity);
        } else if (env.climbable()) {
            result = climbTick(previous, input, box, velocity, env);
        } else {
            result = walkTick(previous, input, context, box, velocity, env);
        }

        return new PlayerState(feetOf(result.box()), result.velocity(),
                previous.yaw(), previous.pitch(), result.onGround());
    }

    // ---- walking (ground + air) -----------------------------------------

    private PhysicsSimulator.Result walkTick(PlayerState previous, MovementInput input,
                                             MovementContext context, Aabb box,
                                             Vec3 velocity, Environment env) {
        boolean onGround = previous.onGround();
        double frictionXz = onGround
                ? MotionFormulas.horizontalFriction(env.slipperiness())
                : MotionFormulas.AIR_FRICTION;

        double acceleration = onGround
                ? MotionFormulas.groundAcceleration(frictionXz, input.isSprintingForward())
                : MotionFormulas.airAcceleration(input.isSprintingForward());

        double moveMultiplier = context.speedEffectMultiplier();
        if (input.sneak() && onGround) {
            moveMultiplier *= SNEAK_MULTIPLIER;
        }
        if (onGround) {
            moveMultiplier *= env.surfaceSpeedMultiplier();
        }
        acceleration *= moveMultiplier;

        Vec3 inputAccel = inputAcceleration(previous.yaw(), input, acceleration);

        Vec3 launch = velocity;
        if (input.jump() && onGround) {
            double jumpVelocity = MotionFormulas.jumpVelocity(context.jumpBoost());
            double launchX = velocity.x();
            double launchZ = velocity.z();
            if (input.isSprintingForward()) {
                double yawRad = Math.toRadians(previous.yaw());
                launchX += -Math.sin(yawRad) * SPRINT_JUMP_IMPULSE;
                launchZ += Math.cos(yawRad) * SPRINT_JUMP_IMPULSE;
            }
            launch = new Vec3(launchX, jumpVelocity, launchZ);
        }

        double wantX = launch.x() * frictionXz + inputAccel.x();
        double wantZ = launch.z() * frictionXz + inputAccel.z();
        double wantY = verticalVelocity(launch.y(), context);

        PhysicsSimulator.Result result =
                simulator.move(box, new Vec3(wantX, wantY, wantZ), onGround, true);

        // Slime bounce: a descent cancelled by a bouncy block is reflected.
        if (result.onGround() && wantY < 0.0 && !input.sneak()
                && simulator.supportingBlock(result.box()).isBouncy()) {
            return new PhysicsSimulator.Result(result.box(),
                    new Vec3(result.velocity().x(), -wantY, result.velocity().z()),
                    false);
        }
        return result;
    }

    /** The vertical velocity for one walking tick, honouring effects. */
    private static double verticalVelocity(double velocityY, MovementContext context) {
        if (context.hasLevitation()) {
            return MotionFormulas.nextVerticalVelocityLevitation(
                    velocityY, context.levitationLevel());
        }
        if (context.slowFalling() && velocityY <= 0.0) {
            return MotionFormulas.nextVerticalVelocitySlowFalling(velocityY);
        }
        return MotionFormulas.nextVerticalVelocity(velocityY);
    }

    // ---- fluids (water / lava) ------------------------------------------

    /**
     * One tick in water or lava. Horizontal motion decays under the
     * fluid drag and is driven by the swim input; in water that drag
     * and acceleration are raised by Depth Strider and overridden by
     * Dolphin's Grace. Vertical motion sinks under buoyant gravity,
     * rises with the jump (swim) impulse, and is overridden by a
     * bubble column current.
     */
    private PhysicsSimulator.Result fluidTick(PlayerState previous, MovementInput input,
                                              Aabb box, Vec3 velocity,
                                              Environment env, MovementContext context) {
        boolean water = env.water();
        double verticalDrag = water ? MotionFormulas.WATER_DRAG : MotionFormulas.LAVA_DRAG;

        double horizontalDrag;
        double acceleration;
        if (water) {
            horizontalDrag = context.dolphinsGrace()
                    ? MotionFormulas.DOLPHINS_GRACE_FRICTION
                    : MotionFormulas.depthStriderWaterFriction(
                            context.depthStrider(), previous.onGround());
            acceleration = MotionFormulas.depthStriderWaterAcceleration(
                    context.depthStrider(), previous.onGround());
        } else {
            horizontalDrag = MotionFormulas.LAVA_DRAG;
            acceleration = MotionFormulas.FLUID_ACCELERATION;
        }

        Vec3 inputAccel = inputAcceleration(previous.yaw(), input, acceleration);

        double wantX = velocity.x() * horizontalDrag + inputAccel.x();
        double wantZ = velocity.z() * horizontalDrag + inputAccel.z();
        double wantY = MotionFormulas.nextVerticalVelocityInFluid(
                velocity.y(), verticalDrag);
        if (input.jump()) {
            wantY += SWIM_UP_IMPULSE;
        }
        wantY = applyBubbleColumn(wantY, env.bubbleColumn());

        return simulator.move(box, new Vec3(wantX, wantY, wantZ),
                previous.onGround(), false);
    }

    /** Vertical velocity after a bubble column's current, if any. */
    private static double applyBubbleColumn(double wantY, BubbleColumn column) {
        return switch (column) {
            case UPWARD -> Math.min(MotionFormulas.BUBBLE_COLUMN_UP_MAX,
                    wantY + MotionFormulas.BUBBLE_COLUMN_UP_ACCELERATION);
            case DOWNWARD -> Math.max(MotionFormulas.BUBBLE_COLUMN_DOWN_MAX,
                    wantY - MotionFormulas.BUBBLE_COLUMN_DOWN_ACCELERATION);
            case NONE -> wantY;
        };
    }

    // ---- powder snow -----------------------------------------------------

    /**
     * One tick sinking through powder snow. Horizontal motion decays
     * under a mild drag and is driven by the air input; the descent is
     * clamped to a slow sink, and a grounded player can still jump
     * free.
     *
     * <p>This models a player without leather boots; a player wearing
     * them stands on the block instead, which the world layer reports
     * as ordinary collision.
     */
    private PhysicsSimulator.Result powderSnowTick(PlayerState previous,
                                                   MovementInput input, Aabb box,
                                                   Vec3 velocity) {
        Vec3 inputAccel = inputAcceleration(previous.yaw(), input,
                MotionFormulas.airAcceleration(false));

        double wantX = velocity.x() * MotionFormulas.POWDER_SNOW_DRAG_HORIZONTAL
                + inputAccel.x();
        double wantZ = velocity.z() * MotionFormulas.POWDER_SNOW_DRAG_HORIZONTAL
                + inputAccel.z();
        double wantY = MotionFormulas.nextVerticalVelocity(velocity.y());
        if (wantY < 0.0) {
            wantY = Math.max(wantY, -MotionFormulas.POWDER_SNOW_DESCENT_CLAMP);
        }
        if (input.jump() && previous.onGround()) {
            wantY = MotionFormulas.jumpVelocity(0);
        }
        return simulator.move(box, new Vec3(wantX, wantY, wantZ),
                previous.onGround(), false);
    }

    /**
     * Velocity after one tick of a firework rocket boost while
     * gliding: each axis is pulled toward {@code 1.5} times the look
     * vector, the impulse Minecraft's rocket entity adds per tick.
     */
    private static Vec3 applyFireworkBoost(Vec3 velocity, Vec3 look) {
        double gain = MotionFormulas.FIREWORK_LOOK_GAIN;
        double target = MotionFormulas.FIREWORK_TARGET_FACTOR;
        double converge = MotionFormulas.FIREWORK_CONVERGENCE;
        return new Vec3(
                velocity.x() + look.x() * gain
                        + (look.x() * target - velocity.x()) * converge,
                velocity.y() + look.y() * gain
                        + (look.y() * target - velocity.y()) * converge,
                velocity.z() + look.z() * gain
                        + (look.z() * target - velocity.z()) * converge);
    }

    // ---- climbing (ladders / vines) -------------------------------------

    private PhysicsSimulator.Result climbTick(PlayerState previous, MovementInput input,
                                              Aabb box, Vec3 velocity, Environment env) {
        Vec3 inputAccel = inputAcceleration(previous.yaw(), input,
                MotionFormulas.airAcceleration(false));

        double clamp = MotionFormulas.CLIMB_HORIZONTAL_CLAMP;
        double wantX = clampAbs(velocity.x() * MotionFormulas.AIR_FRICTION
                + inputAccel.x(), clamp);
        double wantZ = clampAbs(velocity.z() * MotionFormulas.AIR_FRICTION
                + inputAccel.z(), clamp);

        double wantY = Math.max(MotionFormulas.nextVerticalVelocity(velocity.y()),
                -MotionFormulas.CLIMB_FALL_CLAMP);
        if (input.forward() > 0 || input.jump()) {
            wantY = MotionFormulas.CLIMB_UP_SPEED;
        }
        if (input.sneak()) {
            // A ladder clings on sneak; scaffolding is descended through.
            wantY = env.scaffolding() ? -MotionFormulas.CLIMB_FALL_CLAMP : 0.0;
        }
        return simulator.move(box, new Vec3(wantX, wantY, wantZ),
                previous.onGround(), false);
    }

    // ---- elytra ----------------------------------------------------------

    private PhysicsSimulator.Result elytraTick(PlayerState previous, Aabb box,
                                               Vec3 velocity) {
        Vec3 look = previous.rotation().directionVector();
        double pitchRad = Math.toRadians(previous.pitch());
        double horizontalLook = Math.hypot(look.x(), look.z());
        double horizontalMotion = Math.hypot(velocity.x(), velocity.z());
        double lookLength = look.length();

        double cosFactor = Math.cos(pitchRad);
        cosFactor = cosFactor * cosFactor * Math.min(1.0, lookLength / 0.4);

        double mx = velocity.x();
        double my = velocity.y() + MotionFormulas.GRAVITY * (cosFactor * 0.75 - 1.0);
        double mz = velocity.z();

        if (my < 0.0 && horizontalLook > 0.0) {
            double redirected = my * -0.1 * cosFactor;
            my += redirected;
            mx += look.x() * redirected / horizontalLook;
            mz += look.z() * redirected / horizontalLook;
        }
        if (pitchRad < 0.0 && horizontalLook > 0.0) {
            double lift = horizontalMotion * (-Math.sin(pitchRad)) * 0.04;
            my += lift * 3.2;
            mx -= look.x() * lift / horizontalLook;
            mz -= look.z() * lift / horizontalLook;
        }
        if (horizontalLook > 0.0) {
            mx += (look.x() / horizontalLook * horizontalMotion - mx) * 0.1;
            mz += (look.z() / horizontalLook * horizontalMotion - mz) * 0.1;
        }
        mx *= 0.99;
        my *= 0.98;
        mz *= 0.99;
        return simulator.move(box, new Vec3(mx, my, mz), previous.onGround(), false);
    }

    // ---- shared helpers --------------------------------------------------

    /**
     * World-space horizontal acceleration produced by {@code input}
     * for a player looking along {@code yaw}, using Minecraft's
     * {@code moveRelative}: the input vector is normalised against its
     * own magnitude (only when that exceeds one), scaled by
     * {@code acceleration}, and rotated by the yaw.
     */
    private static Vec3 inputAcceleration(float yaw, MovementInput input,
                                          double acceleration) {
        double strafe = input.strafe();
        double forward = input.forward();
        double magnitude = Math.sqrt(strafe * strafe + forward * forward);
        if (magnitude < INPUT_EPSILON) {
            return Vec3.ZERO;
        }
        double scale = acceleration / Math.max(magnitude, 1.0);
        strafe *= scale;
        forward *= scale;

        double yawRad = Math.toRadians(yaw);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        return new Vec3(strafe * cos - forward * sin, 0.0, forward * cos + strafe * sin);
    }

    private static double clampAbs(double value, double limit) {
        return Math.max(-limit, Math.min(limit, value));
    }

    /** Environmental properties of the cells a player box overlaps. */
    private record Environment(boolean water, boolean lava, boolean climbable,
                               boolean cobweb, boolean scaffolding,
                               boolean powderSnow, BubbleColumn bubbleColumn,
                               double slipperiness, double surfaceSpeedMultiplier) {
    }

    /** Scans the world cells {@code box} overlaps and the block below it. */
    private Environment scan(Aabb box) {
        WorldCache world = simulator.collision().world();
        boolean water = false;
        boolean lava = false;
        boolean climbable = false;
        boolean cobweb = false;
        boolean scaffolding = false;
        boolean powderSnow = false;
        BubbleColumn bubbleColumn = BubbleColumn.NONE;
        int minX = (int) Math.floor(box.minX());
        int minY = (int) Math.floor(box.minY());
        int minZ = (int) Math.floor(box.minZ());
        int maxX = (int) Math.floor(box.maxX());
        int maxY = (int) Math.floor(box.maxY());
        int maxZ = (int) Math.floor(box.maxZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState state = world.blockAt(new BlockPos(x, y, z));
                    if (state.fluid() == Fluid.WATER) {
                        water = true;
                    } else if (state.fluid() == Fluid.LAVA) {
                        lava = true;
                    }
                    if (state.isClimbable()) {
                        climbable = true;
                    }
                    if (state.isCobweb()) {
                        cobweb = true;
                    }
                    if (state.isScaffolding()) {
                        scaffolding = true;
                    }
                    if (state.isPowderSnow()) {
                        powderSnow = true;
                    }
                    if (state.bubbleColumn() != BubbleColumn.NONE) {
                        bubbleColumn = state.bubbleColumn();
                    }
                }
            }
        }
        return new Environment(water, lava, climbable, cobweb, scaffolding,
                powderSnow, bubbleColumn, simulator.slipperinessBelow(box),
                simulator.supportingBlock(box).speedMultiplier());
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
