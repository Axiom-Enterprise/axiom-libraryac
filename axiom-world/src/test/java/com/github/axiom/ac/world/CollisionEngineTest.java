package com.github.axiom.ac.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.Ray;
import com.github.axiom.ac.math.Vec3;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CollisionEngineTest {

    private final WorldCache world = new WorldCache();
    private final CollisionEngine engine = new CollisionEngine(world);

    @Test
    void collidesIsFalseInEmptyWorld() {
        assertFalse(engine.collides(new Aabb(0, 0, 0, 1, 1, 1)));
    }

    @Test
    void collidesDetectsOverlappingSolidBlock() {
        world.setBlock(new BlockPos(5, 64, 5), BlockState.SOLID);
        assertTrue(engine.collides(new Aabb(4.7, 64.0, 4.7, 5.3, 65.8, 5.3)));
    }

    @Test
    void collidesIgnoresPassableBlock() {
        world.setBlock(new BlockPos(5, 64, 5), BlockState.PASSABLE);
        assertFalse(engine.collides(new Aabb(5.1, 64.1, 5.1, 5.9, 64.9, 5.9)));
    }

    @Test
    void collidesExcludesBlockTouchedOnlyAtFace() {
        world.setBlock(new BlockPos(5, 64, 5), BlockState.SOLID);
        // Box sits exactly on top of the block: shared face, no overlap.
        assertFalse(engine.collides(new Aabb(5.0, 65.0, 5.0, 5.9, 66.0, 5.9)));
    }

    @Test
    void raycastHitsSolidBlockAhead() {
        world.setBlock(new BlockPos(10, 0, 0), BlockState.SOLID);
        Ray ray = new Ray(new Vec3(0.5, 0.5, 0.5), new Vec3(1, 0, 0));
        Optional<BlockPos> hit = engine.raycast(ray, 20.0);
        assertEquals(Optional.of(new BlockPos(10, 0, 0)), hit);
    }

    @Test
    void raycastMissesWhenNothingInRange() {
        world.setBlock(new BlockPos(100, 0, 0), BlockState.SOLID);
        Ray ray = new Ray(new Vec3(0.5, 0.5, 0.5), new Vec3(1, 0, 0));
        assertTrue(engine.raycast(ray, 20.0).isEmpty());
    }

    @Test
    void raycastReturnsBlockContainingOriginWhenSolid() {
        world.setBlock(new BlockPos(0, 0, 0), BlockState.SOLID);
        Ray ray = new Ray(new Vec3(0.5, 0.5, 0.5), new Vec3(1, 0, 0));
        assertEquals(Optional.of(new BlockPos(0, 0, 0)), engine.raycast(ray, 5.0));
    }

    @Test
    void raycastStopsAtFirstSolidBlock() {
        world.setBlock(new BlockPos(3, 0, 0), BlockState.SOLID);
        world.setBlock(new BlockPos(7, 0, 0), BlockState.SOLID);
        Ray ray = new Ray(new Vec3(0.5, 0.5, 0.5), new Vec3(1, 0, 0));
        assertEquals(Optional.of(new BlockPos(3, 0, 0)), engine.raycast(ray, 20.0));
    }
}
