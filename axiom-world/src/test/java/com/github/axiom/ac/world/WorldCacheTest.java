package com.github.axiom.ac.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorldCacheTest {

    @Test
    void uncachedBlockIsUnknown() {
        assertEquals(BlockState.UNKNOWN, new WorldCache().blockAt(new BlockPos(0, 0, 0)));
    }

    @Test
    void storedBlockIsReadBack() {
        WorldCache cache = new WorldCache();
        BlockPos pos = new BlockPos(1, 64, 2);
        cache.setBlock(pos, BlockState.SOLID);
        assertEquals(BlockState.SOLID, cache.blockAt(pos));
    }

    @Test
    void isSolidReflectsState() {
        WorldCache cache = new WorldCache();
        BlockPos solid = new BlockPos(1, 1, 1);
        BlockPos passable = new BlockPos(2, 2, 2);
        cache.setBlock(solid, BlockState.SOLID);
        cache.setBlock(passable, BlockState.PASSABLE);
        assertTrue(cache.isSolid(solid));
        assertFalse(cache.isSolid(passable));
        assertFalse(cache.isSolid(new BlockPos(9, 9, 9)));
    }

    @Test
    void settingUnknownForgetsTheBlock() {
        WorldCache cache = new WorldCache();
        BlockPos pos = new BlockPos(3, 3, 3);
        cache.setBlock(pos, BlockState.SOLID);
        cache.setBlock(pos, BlockState.UNKNOWN);
        assertEquals(BlockState.UNKNOWN, cache.blockAt(pos));
        assertEquals(0, cache.size());
    }

    @Test
    void clearForgetsEveryBlock() {
        WorldCache cache = new WorldCache();
        cache.setBlock(new BlockPos(0, 0, 0), BlockState.SOLID);
        cache.setBlock(new BlockPos(1, 0, 0), BlockState.SOLID);
        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    void rejectsNullArguments() {
        WorldCache cache = new WorldCache();
        assertThrows(NullPointerException.class,
                () -> cache.setBlock(null, BlockState.SOLID));
        assertThrows(NullPointerException.class,
                () -> cache.setBlock(new BlockPos(0, 0, 0), null));
    }
}
