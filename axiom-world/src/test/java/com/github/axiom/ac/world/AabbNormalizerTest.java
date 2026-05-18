package com.github.axiom.ac.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Aabb;
import org.junit.jupiter.api.Test;

class AabbNormalizerTest {

    @Test
    void normalizeOrdersAnInvertedBox() {
        Aabb inverted = new Aabb(1, 1, 1, 0, 0, 0);
        Aabb ordered = AabbNormalizer.normalize(inverted);
        assertEquals(new Aabb(0, 0, 0, 1, 1, 1), ordered);
        assertTrue(AabbNormalizer.isCanonical(ordered));
    }

    @Test
    void normalizeLeavesAnOrderedBoxUntouched() {
        Aabb ordered = new Aabb(0, 0, 0, 1, 2, 3);
        assertSame(ordered, AabbNormalizer.normalize(ordered));
    }

    @Test
    void isCanonicalDetectsAnInvertedAxis() {
        assertFalse(AabbNormalizer.isCanonical(new Aabb(0, 5, 0, 1, 2, 1)));
    }

    @Test
    void cellLocalClampsAndOrders() {
        Aabb wild = new Aabb(-3, 0.2, 9, 0.5, 0.8, -1);
        Aabb local = AabbNormalizer.cellLocal(wild);
        assertTrue(AabbNormalizer.isCellLocal(local));
        assertEquals(new Aabb(0.0, 0.2, 0.0, 0.5, 0.8, 1.0), local);
    }

    @Test
    void isCellLocalRejectsAnOutOfCellBox() {
        assertFalse(AabbNormalizer.isCellLocal(new Aabb(0, 0, 0, 1, 2, 1)));
        assertTrue(AabbNormalizer.isCellLocal(new Aabb(0, 0, 0, 1, 1, 1)));
    }
}
