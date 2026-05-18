package com.github.axiom.ac.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import com.github.axiom.ac.world.BlockPos;
import com.github.axiom.ac.world.BlockState;
import com.github.axiom.ac.world.CollisionEngine;
import com.github.axiom.ac.world.PhysicsSimulator;
import com.github.axiom.ac.world.WorldCache;
import org.junit.jupiter.api.Test;

class PredictionEngineTest {

    /** Engine over an empty world (no blocks, nothing collides). */
    private static PredictionEngine engineInEmptyWorld() {
        PhysicsSimulator simulator = new PhysicsSimulator(new CollisionEngine(new WorldCache()));
        return new PredictionEngine(simulator);
    }

    /** Engine over the given world. */
    private static PredictionEngine engineOver(WorldCache world) {
        return new PredictionEngine(new PhysicsSimulator(new CollisionEngine(world)));
    }

    /** Fills the cells a player box at {@code feet} overlaps with {@code state}. */
    private static void fillAround(WorldCache world, Vec3 feet, BlockState state) {
        for (int x = -1; x <= 0; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -1; z <= 0; z++) {
                    world.setBlock(new BlockPos(
                            (int) feet.x() + x,
                            (int) feet.y() + y,
                            (int) feet.z() + z), state);
                }
            }
        }
    }

    @Test
    void rejectsNullSimulator() {
        assertThrows(NullPointerException.class, () -> new PredictionEngine(null));
    }

    @Test
    void neutralInputJustFallsUnderGravity() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, false);

        PlayerState next = engine.predict(start, MovementInput.none());

        // No horizontal input -> no horizontal motion.
        assertEquals(0.0, next.position().x(), 1e-9);
        assertEquals(0.0, next.position().z(), 1e-9);
        // Gravity pulls the player down.
        assertTrue(next.position().y() < 100.0);
    }

    @Test
    void forwardInputAtZeroYawMovesAlongPositiveZ() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, false);

        PlayerState next = engine.predict(start, new MovementInput(1, 0, false, false));

        // At yaw 0, "forward" is +Z in Minecraft's convention.
        assertTrue(next.position().z() > 0.0);
        assertEquals(0.0, next.position().x(), 1e-9);
    }

    @Test
    void sprintForwardOutrunsWalkForward() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, false);

        double walk = engine.predict(start,
                new MovementInput(1, 0, false, false)).position().z();
        double sprint = engine.predict(start,
                new MovementInput(1, 0, false, true)).position().z();

        assertTrue(sprint > walk);
    }

    @Test
    void jumpFromGroundProducesUpwardMotion() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, true);

        PlayerState next = engine.predict(start, new MovementInput(0, 0, true, false));

        // A jump impulse beats one tick of gravity: the player rises.
        assertTrue(next.position().y() > 100.0);
    }

    @Test
    void jumpInTheAirDoesNotRise() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, false);

        PlayerState next = engine.predict(start, new MovementInput(0, 0, true, false));

        // Not on the ground: the jump key does nothing, gravity wins.
        assertTrue(next.position().y() < 100.0);
    }

    @Test
    void sneakingSlowsGroundMovement() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 64, 0), new Vec3(0, 0, 0), 0.0f, true);

        double walk = engine.predict(start,
                new MovementInput(1, 0, false, false, false)).position().z();
        double sneak = engine.predict(start,
                new MovementInput(1, 0, false, false, true)).position().z();

        assertTrue(sneak < walk);
        assertTrue(sneak > 0.0);
    }

    @Test
    void speedEffectOutrunsAPlainSprint() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 64, 0), new Vec3(0, 0, 0), 0.0f, true);
        MovementInput sprint = new MovementInput(1, 0, false, true);

        double plain = engine.predict(start, sprint, MovementContext.none())
                .position().z();
        double hasted = engine.predict(start, sprint,
                new MovementContext(0, 1, 0, 0, false, false)).position().z();

        assertTrue(hasted > plain);
    }

    @Test
    void levitationLiftsAnAirbornePlayer() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, false);

        PlayerState next = engine.predict(start, MovementInput.none(),
                new MovementContext(0, 0, 0, 1, false, false));

        assertTrue(next.position().y() > 100.0);
    }

    @Test
    void slowFallingSoftensTheDescent() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, -0.5, 0), 0.0f, false);

        double plain = engine.predict(start, MovementInput.none(),
                MovementContext.none()).position().y();
        double slow = engine.predict(start, MovementInput.none(),
                new MovementContext(0, 0, 0, 0, true, false)).position().y();

        assertTrue(slow > plain);
    }

    @Test
    void waterMakesThePlayerSinkSlowly() {
        WorldCache world = new WorldCache();
        Vec3 feet = new Vec3(0, 64, 0);
        fillAround(world, feet, BlockState.WATER);
        PredictionEngine engine = engineOver(world);
        PlayerState start = new PlayerState(feet, new Vec3(0, 0, 0), 0.0f, false);

        PlayerState next = engine.predict(start, MovementInput.none());

        // Submerged: descends, but far slower than a free fall (0.0784).
        assertTrue(next.position().y() < 64.0);
        assertTrue(64.0 - next.position().y() < 0.05);
    }

    @Test
    void climbingALadderRisesWhenHoldingForward() {
        WorldCache world = new WorldCache();
        Vec3 feet = new Vec3(0, 64, 0);
        fillAround(world, feet, BlockState.LADDER);
        PredictionEngine engine = engineOver(world);
        PlayerState start = new PlayerState(feet, new Vec3(0, 0, 0), 0.0f, false);

        PlayerState next = engine.predict(start, new MovementInput(1, 0, false, false));

        assertTrue(next.position().y() > 64.0);
    }

    @Test
    void slimeBlockBouncesAFallingPlayer() {
        WorldCache world = new WorldCache();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlock(new BlockPos(x, 63, z), BlockState.SLIME_BLOCK);
            }
        }
        PredictionEngine engine = engineOver(world);
        PlayerState start = new PlayerState(
                new Vec3(0, 64, 0), new Vec3(0, -0.3, 0), 0.0f, true);

        PlayerState next = engine.predict(start, MovementInput.none());

        // The cancelled descent is reflected into an upward bounce.
        assertTrue(next.velocity().y() > 0.0);
    }

    @Test
    void elytraGlideTurnsLookIntoForwardMotion() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0.5, -0.1, 0), 0.0f, 0.0f, false);

        PlayerState next = engine.predict(start, MovementInput.none(),
                new MovementContext(0, 0, 0, 0, false, true));

        // Looking along +Z, the glide steers motion forward.
        assertTrue(next.position().z() > 0.0);
        assertTrue(Double.isFinite(next.position().y()));
    }

    @Test
    void fireworkBoostAcceleratesAnElytraGlide() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0.2), 0.0f, 0.0f, false);

        double plain = engine.predict(start, MovementInput.none(),
                MovementContext.builder().elytra(true).build()).position().z();
        double boosted = engine.predict(start, MovementInput.none(),
                MovementContext.builder().elytra(true).fireworkBoost(true).build())
                .position().z();

        // The rocket pulls the glide toward the +Z look vector.
        assertTrue(boosted > plain);
    }

    @Test
    void riptideLaunchesThePlayerAlongTheLookVector() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, 0.0f, false);

        PlayerState next = engine.predict(start, MovementInput.none(),
                MovementContext.builder().riptideLevel(3).build());

        // Level 3 adds three blocks/tick along +Z, far past a still stand.
        assertTrue(next.position().z() > 2.0);
    }

    @Test
    void depthStriderSpeedsUpSwimming() {
        WorldCache world = new WorldCache();
        Vec3 feet = new Vec3(0, 64, 0);
        fillAround(world, feet, BlockState.WATER);
        PredictionEngine engine = engineOver(world);
        PlayerState start = new PlayerState(feet, new Vec3(0, 0, 0), 0.0f, true);
        MovementInput forward = new MovementInput(1, 0, false, false);

        double plain = engine.predict(start, forward, MovementContext.none())
                .position().z();
        double strider = engine.predict(start, forward,
                MovementContext.builder().depthStrider(3).build()).position().z();

        assertTrue(strider > plain);
    }

    @Test
    void anUpwardBubbleColumnLiftsThePlayer() {
        WorldCache world = new WorldCache();
        Vec3 feet = new Vec3(0, 64, 0);
        fillAround(world, feet, BlockState.BUBBLE_COLUMN_UPWARD);
        PredictionEngine engine = engineOver(world);
        PlayerState start = new PlayerState(feet, new Vec3(0, 0, 0), 0.0f, false);

        PlayerState next = engine.predict(start, MovementInput.none());

        assertTrue(next.position().y() > 64.0);
    }

    @Test
    void powderSnowSlowsTheDescentToASink() {
        WorldCache world = new WorldCache();
        Vec3 feet = new Vec3(0, 64, 0);
        fillAround(world, feet, BlockState.POWDER_SNOW);
        PredictionEngine engine = engineOver(world);
        PlayerState start = new PlayerState(feet, new Vec3(0, 0, 0), 0.0f, false);

        PlayerState next = engine.predict(start, MovementInput.none());

        // Sinks, but slower than the 0.0784 of a first tick of free fall.
        assertTrue(next.position().y() < 64.0);
        assertTrue(64.0 - next.position().y() < 0.0784);
    }

    @Test
    void scaffoldingIsDescendedThroughOnSneak() {
        WorldCache world = new WorldCache();
        Vec3 feet = new Vec3(0, 64, 0);
        fillAround(world, feet, BlockState.SCAFFOLDING);
        PredictionEngine engine = engineOver(world);
        PlayerState start = new PlayerState(feet, new Vec3(0, 0, 0), 0.0f, false);

        double hold = engine.predict(start,
                new MovementInput(0, 0, false, false, false)).position().y();
        double sneak = engine.predict(start,
                new MovementInput(0, 0, false, false, true)).position().y();

        assertTrue(sneak < hold);
    }
}
