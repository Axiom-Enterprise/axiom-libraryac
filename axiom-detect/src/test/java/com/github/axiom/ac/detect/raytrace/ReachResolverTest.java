package com.github.axiom.ac.detect.raytrace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.Vec3;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReachResolverTest {

    private static final double EPS = 1e-9;

    private final Aabb target = new Aabb(2, 0, 0, 3, 1, 1);
    private final Vec3 eye = new Vec3(0, 0.5, 0.5);

    @Test
    void distanceToTargetAlongTheLook() {
        assertEquals(2.0, ReachResolver.distance(eye, new Vec3(1, 0, 0), target).orElseThrow(), EPS);
    }

    @Test
    void directionNeedNotBeNormalised() {
        assertEquals(2.0, ReachResolver.distance(eye, new Vec3(5, 0, 0), target).orElseThrow(), EPS);
    }

    @Test
    void emptyWhenTheLookMissesTheBox() {
        assertTrue(ReachResolver.distance(eye, new Vec3(0, 1, 0), target).isEmpty());
    }

    @Test
    void minimumAcrossEyePositionsIsTheMostForgiving() {
        List<Vec3> eyes = List.of(new Vec3(0, 0.5, 0.5), new Vec3(1, 0.5, 0.5));
        assertEquals(1.0, ReachResolver.minimumDistance(eyes, new Vec3(1, 0, 0), target).orElseThrow(), EPS);
    }

    @Test
    void minimumIsEmptyWhenEveryEyeMisses() {
        List<Vec3> eyes = List.of(new Vec3(0, 0.5, 0.5), new Vec3(1, 0.5, 0.5));
        assertTrue(ReachResolver.minimumDistance(eyes, new Vec3(0, 1, 0), target).isEmpty());
    }
}
