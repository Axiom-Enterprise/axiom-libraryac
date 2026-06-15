package com.github.axiom.ac.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.axiom.ac.math.Vec3;
import org.junit.jupiter.api.Test;

class VoxelNormalizerTest {

    private static final double EPS = 1e-9;

    @Test
    void blockAtSnapsNearIntegerDriftUpToTheBoundary() {
        // A true 5.0 that drifted just below must resolve to block 5.
        BlockPos pos = VoxelNormalizer.blockAt(new Vec3(4.9999999, 70.0, 4.9999999));
        assertEquals(new BlockPos(5, 70, 5), pos);
    }

    @Test
    void blockAtFloorsAGenuineInteriorCoordinate() {
        BlockPos pos = VoxelNormalizer.blockAt(new Vec3(4.5, 70.3, 4.9));
        assertEquals(new BlockPos(4, 70, 4), pos);
    }

    @Test
    void blockAtFloorsNegativeCoordinates() {
        BlockPos pos = VoxelNormalizer.blockAt(new Vec3(-0.5, 0.0, -8.5));
        assertEquals(new BlockPos(-1, 0, -9), pos);
    }

    @Test
    void snapPullsInANearIntegerOnly() {
        assertEquals(7.0, VoxelNormalizer.snap(7.00005, 1e-4), EPS);
        assertEquals(7.4, VoxelNormalizer.snap(7.4, 1e-4), EPS);
    }

    @Test
    void snapRejectsANegativeEpsilon() {
        assertThrows(IllegalArgumentException.class,
                () -> VoxelNormalizer.snap(1.0, -1.0));
    }

    @Test
    void chunkLocalFoldsCoordinatesIntoZeroToFifteen() {
        assertEquals(5, VoxelNormalizer.chunkLocal(5));
        assertEquals(0, VoxelNormalizer.chunkLocal(16));
        assertEquals(15, VoxelNormalizer.chunkLocal(-1));
    }

    @Test
    void chunkOfIndexesTheContainingChunk() {
        assertEquals(0, VoxelNormalizer.chunkOf(15));
        assertEquals(1, VoxelNormalizer.chunkOf(16));
        assertEquals(-1, VoxelNormalizer.chunkOf(-1));
    }

    @Test
    void chunkLocalBlockPosKeepsYAndFoldsXz() {
        BlockPos local = VoxelNormalizer.chunkLocal(new BlockPos(-1, 70, 33));
        assertEquals(new BlockPos(15, 70, 1), local);
    }
}
