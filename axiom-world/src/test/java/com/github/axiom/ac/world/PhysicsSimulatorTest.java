package com.github.axiom.ac.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.Vec3;
import org.junit.jupiter.api.Test;

class PhysicsSimulatorTest {

    private static final double EPS = 1e-9;

    private final WorldCache world = new WorldCache();
    private final CollisionEngine engine = new CollisionEngine(world);
    private final PhysicsSimulator simulator = new PhysicsSimulator(engine);

    /** Player box 0.6 wide, 1.8 tall, resting with its base at y. */
    private static Aabb playerBox(double x, double y, double z) {
        return new Aabb(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3);
    }

    private void floorAt(int y) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlock(new BlockPos(x, y, z), BlockState.SOLID);
            }
        }
    }

    @Test
    void fallsUnderGravityInEmptyWorld() {
        Aabb box = playerBox(0.5, 100.0, 0.5);
        PhysicsSimulator.Result result = simulator.simulate(box, new Vec3(0, 0, 0), false);
        // Vertical velocity from rest: (0 - 0.08) * 0.98 = -0.0784.
        assertEquals(-0.0784, result.velocity().y(), EPS);
        assertTrue(result.box().minY() < 100.0);
        assertFalse(result.onGround());
    }

    @Test
    void solidFloorStopsTheFallAndSupportsThePlayer() {
        floorAt(63);
        // Player resting exactly on top of the floor, at y = 64.
        Aabb box = playerBox(0.5, 64.0, 0.5);
        PhysicsSimulator.Result result = simulator.simulate(box, new Vec3(0, 0, 0), true);

        assertEquals(64.0, result.box().minY(), EPS);
        assertEquals(0.0, result.velocity().y(), EPS);
        assertTrue(result.onGround());
    }

    @Test
    void airborneHorizontalVelocityDecaysWithBareAirFriction() {
        Aabb box = playerBox(0.5, 100.0, 0.5);
        PhysicsSimulator.Result result = simulator.simulate(box, new Vec3(1.0, 0, 0), false);
        // Airborne: friction is the bare air factor 0.91, with no
        // surface slipperiness applied.
        assertEquals(0.91, result.velocity().x(), EPS);
    }

    @Test
    void groundedHorizontalVelocityDecaysWithSurfaceFriction() {
        Aabb box = playerBox(0.5, 100.0, 0.5);
        PhysicsSimulator.Result result = simulator.simulate(box, new Vec3(1.0, 0, 0), true);
        // Grounded over the default surface: 0.6 * 0.91 = 0.546.
        assertEquals(0.546, result.velocity().x(), EPS);
    }

    @Test
    void iceSurfaceRetainsMoreHorizontalVelocity() {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlock(new BlockPos(x, 63, z), BlockState.ICE);
            }
        }
        Aabb box = playerBox(0.5, 64.0, 0.5);
        PhysicsSimulator.Result result = simulator.simulate(box, new Vec3(1.0, 0, 0), true);
        // Ice slipperiness 0.98: 0.98 * 0.91 = 0.8918.
        assertEquals(0.8918, result.velocity().x(), EPS);
    }

    @Test
    void inputAccelerationIsAddedAfterFrictionDecay() {
        Aabb box = playerBox(0.5, 100.0, 0.5);
        PhysicsSimulator.Result result = simulator.simulate(
                box, new Vec3(1.0, 0, 0), new Vec3(0.2, 0, 0), false);
        // Friction decays the carried velocity, then input is added
        // undamped: 1.0 * 0.91 + 0.2 = 1.11.
        assertEquals(1.11, result.velocity().x(), EPS);
    }

    @Test
    void explicitSlipperinessOverridesTheSurfaceLookup() {
        Aabb box = playerBox(0.5, 100.0, 0.5);
        PhysicsSimulator.Result result = simulator.simulate(
                box, new Vec3(1.0, 0, 0), Vec3.ZERO, true, 0.98);
        assertEquals(0.8918, result.velocity().x(), EPS);
    }

    @Test
    void solidWallCancelsHorizontalMotion() {
        // Wall of solid blocks at x = 1 around the player's height.
        for (int y = 99; y <= 102; y++) {
            world.setBlock(new BlockPos(1, y, 0), BlockState.SOLID);
        }
        // Player just west of the wall, moving east into it.
        Aabb box = playerBox(0.7, 100.0, 0.5);
        PhysicsSimulator.Result result = simulator.simulate(box, new Vec3(1.0, 0, 0), false);

        // Eastward motion is cancelled; the box does not enter x >= 1.
        assertTrue(result.box().maxX() <= 1.0 + EPS);
        assertEquals(0.0, result.velocity().x(), EPS);
    }

    @Test
    void stepsUpOntoASlabLedge() {
        // Floor at y = 63; a slab ledge at y = 64 just east of the player.
        world.setBlock(new BlockPos(0, 63, 0), BlockState.SOLID);
        world.setBlock(new BlockPos(1, 63, 0), BlockState.SOLID);
        world.setBlock(new BlockPos(1, 64, 0), BlockState.BOTTOM_SLAB);

        Aabb box = playerBox(0.5, 64.0, 0.5);
        PhysicsSimulator.Result result =
                simulator.move(box, new Vec3(0.5, 0, 0), true, true);

        // The player steps onto the slab top (y = 64.5) and keeps the move.
        assertEquals(64.5, result.box().minY(), EPS);
        assertEquals(0.5, result.velocity().x(), EPS);
    }

    @Test
    void doesNotStepUpWhileAirborne() {
        world.setBlock(new BlockPos(1, 64, 0), BlockState.BOTTOM_SLAB);
        Aabb box = playerBox(0.5, 64.0, 0.5);
        PhysicsSimulator.Result result =
                simulator.move(box, new Vec3(0.5, 0, 0), false, true);

        // Airborne: the ledge is a wall, not a step.
        assertEquals(64.0, result.box().minY(), EPS);
        assertEquals(0.0, result.velocity().x(), EPS);
    }

    @Test
    void moveWithoutStepAssistTreatsALedgeAsAWall() {
        world.setBlock(new BlockPos(0, 63, 0), BlockState.SOLID);
        world.setBlock(new BlockPos(1, 64, 0), BlockState.BOTTOM_SLAB);
        Aabb box = playerBox(0.5, 64.0, 0.5);
        PhysicsSimulator.Result result =
                simulator.move(box, new Vec3(0.5, 0, 0), true, false);

        assertEquals(64.0, result.box().minY(), EPS);
        assertEquals(0.0, result.velocity().x(), EPS);
    }

    @Test
    void slabFloorSupportsThePlayerAtHalfHeight() {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlock(new BlockPos(x, 63, z), BlockState.BOTTOM_SLAB);
            }
        }
        // Player resting on the slab top, at y = 63.5.
        Aabb box = playerBox(0.5, 63.5, 0.5);
        PhysicsSimulator.Result result = simulator.simulate(box, new Vec3(0, 0, 0), true);

        assertEquals(63.5, result.box().minY(), EPS);
        assertTrue(result.onGround());
    }
}
