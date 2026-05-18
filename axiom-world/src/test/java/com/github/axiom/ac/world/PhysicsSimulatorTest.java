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
        // Floor of solid blocks at y = 63 (cube spans 63..64).
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlock(new BlockPos(x, 63, z), BlockState.SOLID);
            }
        }
        // Player resting exactly on top of the floor, at y = 64.
        Aabb box = playerBox(0.5, 64.0, 0.5);
        PhysicsSimulator.Result result = simulator.simulate(box, new Vec3(0, 0, 0), true);

        assertEquals(64.0, result.box().minY(), EPS);
        assertEquals(0.0, result.velocity().y(), EPS);
        assertTrue(result.onGround());
    }

    @Test
    void horizontalVelocityDecaysWithFriction() {
        Aabb box = playerBox(0.5, 100.0, 0.5);
        PhysicsSimulator.Result result = simulator.simulate(box, new Vec3(1.0, 0, 0), false);
        // Horizontal friction: 0.6 * 0.91 = 0.546.
        assertEquals(0.546, result.velocity().x(), EPS);
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
}
