package com.github.axiom.ac.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
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
}
