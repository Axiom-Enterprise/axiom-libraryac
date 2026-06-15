package com.github.axiom.ac.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Aabb;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShapeNormalizerTest {

    private static final Aabb FULL = new Aabb(0, 0, 0, 1, 1, 1);

    @Test
    void dropsDegenerateBoxes() {
        Aabb flat = new Aabb(0, 0.5, 0, 1, 0.5, 1);
        assertEquals(List.of(), ShapeNormalizer.normalize(List.of(flat)));
    }

    @Test
    void dropsDuplicateBoxes() {
        assertEquals(List.of(FULL),
                ShapeNormalizer.normalize(List.of(FULL, FULL)));
    }

    @Test
    void dropsBoxesContainedInAnother() {
        Aabb inner = new Aabb(0.2, 0.2, 0.2, 0.8, 0.8, 0.8);
        assertEquals(List.of(FULL),
                ShapeNormalizer.normalize(List.of(FULL, inner)));
    }

    @Test
    void clampsBoxesIntoTheUnitCell() {
        Aabb overflowing = new Aabb(-1, 0, -1, 2, 0.5, 2);
        assertEquals(List.of(new Aabb(0, 0, 0, 1, 0.5, 1)),
                ShapeNormalizer.normalize(List.of(overflowing)));
    }

    @Test
    void keepsDisjointBoxesAndSortsThem() {
        Aabb top = new Aabb(0, 0.5, 0, 1, 1, 1);
        Aabb bottom = new Aabb(0, 0, 0, 1, 0.5, 1);
        assertEquals(List.of(bottom, top),
                ShapeNormalizer.normalize(List.of(top, bottom)));
    }

    @Test
    void isCanonicalRecognisesAnAlreadyCleanShape() {
        assertTrue(ShapeNormalizer.isCanonical(List.of(FULL)));
        assertFalse(ShapeNormalizer.isCanonical(List.of(FULL, FULL)));
    }

    @Test
    void anEmptyShapeNormalizesToEmpty() {
        assertEquals(List.of(), ShapeNormalizer.normalize(List.of()));
    }
}
