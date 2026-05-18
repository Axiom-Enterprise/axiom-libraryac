package com.github.axiom.ac.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.axiom.ac.math.Vec3;
import org.junit.jupiter.api.Test;

class BlockPosTest {

    @Test
    void exposesCoordinates() {
        BlockPos pos = new BlockPos(1, 2, 3);
        assertEquals(1, pos.x());
        assertEquals(2, pos.y());
        assertEquals(3, pos.z());
    }

    @Test
    void ofFloorsPositiveCoordinates() {
        assertEquals(new BlockPos(1, 64, 2), BlockPos.of(new Vec3(1.7, 64.9, 2.1)));
    }

    @Test
    void ofFloorsNegativeCoordinates() {
        assertEquals(new BlockPos(-2, -1, -3), BlockPos.of(new Vec3(-1.3, -0.1, -2.5)));
    }

    @Test
    void ofIsExactOnIntegerBoundaries() {
        assertEquals(new BlockPos(5, 0, -7), BlockPos.of(new Vec3(5.0, 0.0, -7.0)));
    }
}
