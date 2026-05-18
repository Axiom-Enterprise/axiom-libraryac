package com.github.axiom.ac.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Aabb;
import org.junit.jupiter.api.Test;

class BlockStateTest {

    @Test
    void solidIsAFullCubeWithDefaultSlipperiness() {
        assertTrue(BlockState.SOLID.hasCollision());
        assertEquals(1, BlockState.SOLID.collisionBoxes().size());
        assertEquals(0.6, BlockState.SOLID.slipperiness(), 1e-9);
    }

    @Test
    void passableHasNoCollisionAndIsNotUnknown() {
        assertFalse(BlockState.PASSABLE.hasCollision());
        assertTrue(BlockState.PASSABLE.isPassable());
        assertFalse(BlockState.PASSABLE.isUnknown());
    }

    @Test
    void unknownIsDistinctFromPassable() {
        assertTrue(BlockState.UNKNOWN.isUnknown());
        assertFalse(BlockState.UNKNOWN.isPassable());
        assertFalse(BlockState.UNKNOWN.hasCollision());
    }

    @Test
    void iceVariantsAreSlipperierThanDefault() {
        assertTrue(BlockState.ICE.slipperiness() > BlockState.SOLID.slipperiness());
        assertTrue(BlockState.BLUE_ICE.slipperiness() > BlockState.ICE.slipperiness());
    }

    @Test
    void slabsOccupyHalfTheCell() {
        Aabb bottom = BlockState.BOTTOM_SLAB.collisionBoxes().get(0);
        assertEquals(0.0, bottom.minY(), 1e-9);
        assertEquals(0.5, bottom.maxY(), 1e-9);
        Aabb top = BlockState.TOP_SLAB.collisionBoxes().get(0);
        assertEquals(0.5, top.minY(), 1e-9);
        assertEquals(1.0, top.maxY(), 1e-9);
    }

    @Test
    void cubeFactoryBuildsAFullCube() {
        BlockState basalt = BlockState.cube("basalt", 0.6);
        assertTrue(basalt.hasCollision());
        assertEquals("basalt", basalt.name());
    }

    @Test
    void shapeFactoryRejectsBoxesOutsideTheCell() {
        assertThrows(IllegalArgumentException.class,
                () -> BlockState.shape("oversized", 0.6, new Aabb(0, 0, 0, 1, 1.5, 1)));
    }

    @Test
    void slipperinessMustBeInRange() {
        assertThrows(IllegalArgumentException.class, () -> BlockState.cube("bad", 0.0));
        assertThrows(IllegalArgumentException.class, () -> BlockState.cube("bad", 1.5));
    }

    @Test
    void emptyShapeIsPassable() {
        assertTrue(BlockState.shape("air_like", 0.6).isPassable());
    }
}
