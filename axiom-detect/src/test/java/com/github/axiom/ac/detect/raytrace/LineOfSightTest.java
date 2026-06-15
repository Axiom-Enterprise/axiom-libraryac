package com.github.axiom.ac.detect.raytrace;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import com.github.axiom.ac.world.BlockPos;
import com.github.axiom.ac.world.BlockState;
import com.github.axiom.ac.world.CollisionEngine;
import com.github.axiom.ac.world.WorldCache;
import org.junit.jupiter.api.Test;

class LineOfSightTest {

    private final WorldCache world = new WorldCache();
    private final CollisionEngine collision = new CollisionEngine(world);

    @Test
    void clearAcrossEmptyWorld() {
        assertTrue(LineOfSight.clear(new Vec3(0.5, 0.5, 0.5), new Vec3(5.5, 0.5, 0.5), collision));
    }

    @Test
    void blockedByASolidBetween() {
        world.setBlock(new BlockPos(3, 0, 0), BlockState.SOLID);
        assertFalse(LineOfSight.clear(new Vec3(0.5, 0.5, 0.5), new Vec3(5.5, 0.5, 0.5), collision));
    }

    @Test
    void aSolidBeyondTheTargetDoesNotBlock() {
        world.setBlock(new BlockPos(4, 0, 0), BlockState.SOLID);
        assertTrue(LineOfSight.clear(new Vec3(0.5, 0.5, 0.5), new Vec3(2.5, 0.5, 0.5), collision));
    }

    @Test
    void coincidentPointsAreClear() {
        assertTrue(LineOfSight.clear(new Vec3(1, 1, 1), new Vec3(1, 1, 1), collision));
    }
}
