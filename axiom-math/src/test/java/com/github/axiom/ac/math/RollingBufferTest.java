package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RollingBufferTest {

    @Test
    void rejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new RollingBuffer<Integer>(0));
    }

    @Test
    void addsUpToCapacity() {
        RollingBuffer<Integer> buffer = new RollingBuffer<>(3);
        buffer.add(1);
        buffer.add(2);
        assertEquals(2, buffer.size());
        assertFalse(buffer.isFull());
        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
    }

    @Test
    void evictsOldestWhenFull() {
        RollingBuffer<Integer> buffer = new RollingBuffer<>(3);
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        buffer.add(4);
        assertTrue(buffer.isFull());
        assertEquals(3, buffer.size());
        assertEquals(List.of(2, 3, 4), buffer.toList());
    }

    @Test
    void getOutOfRangeThrows() {
        RollingBuffer<Integer> buffer = new RollingBuffer<>(3);
        buffer.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(1));
    }

    @Test
    void capacityIsReported() {
        assertEquals(5, new RollingBuffer<Integer>(5).capacity());
    }
}
