package com.github.axiom.ac.detect.raytrace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.Ray;
import com.github.axiom.ac.math.Vec3;
import com.github.axiom.ac.world.BlockPos;
import com.github.axiom.ac.world.BlockState;
import com.github.axiom.ac.world.CollisionEngine;
import com.github.axiom.ac.world.WorldCache;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RaytraceEngineTest {

    private static final double EPS = 1e-9;

    private final WorldCache world = new WorldCache();
    private final RaytraceEngine engine = new RaytraceEngine(new CollisionEngine(world));

    private final Hitbox<String> near = new Hitbox<>("near", new Aabb(2, 0, 0, 3, 1, 1));
    private final Hitbox<String> far = new Hitbox<>("far", new Aabb(5, 0, 0, 6, 1, 1));
    private final Vec3 eye = new Vec3(0, 0.5, 0.5);

    @Test
    void findsTheHitboxAhead() {
        Ray ray = new Ray(eye, new Vec3(1, 0, 0));
        RayHit<String> hit = engine.nearest(ray, 10.0, List.of(near)).orElseThrow();
        assertEquals("near", hit.hitbox().target());
        assertEquals(2.0, hit.distance(), EPS);
        assertEquals(2.0, hit.point().x(), EPS);
    }

    @Test
    void respectsMaxDistance() {
        Ray ray = new Ray(eye, new Vec3(1, 0, 0));
        assertTrue(engine.nearest(ray, 1.0, List.of(near)).isEmpty());
    }

    @Test
    void picksTheClosestOfSeveral() {
        Ray ray = new Ray(eye, new Vec3(1, 0, 0));
        RayHit<String> hit = engine.nearest(ray, 10.0, List.of(far, near)).orElseThrow();
        assertEquals("near", hit.hitbox().target());
    }

    @Test
    void distanceIsWorldSpaceForANonUnitDirection() {
        // Direction length 2: the slab parameter is 1, but the reported distance is 2 blocks.
        Ray ray = new Ray(eye, new Vec3(2, 0, 0));
        RayHit<String> hit = engine.nearest(ray, 10.0, List.of(near)).orElseThrow();
        assertEquals(2.0, hit.distance(), EPS);
        assertEquals(2.0, hit.point().x(), EPS);
    }

    @Test
    void aDirectionlessRayHitsNothing() {
        Ray ray = new Ray(eye, new Vec3(0, 0, 0));
        assertTrue(engine.nearest(ray, 10.0, List.of(near)).isEmpty());
    }

    @Test
    void visibleHitInAnEmptyWorld() {
        Optional<RayHit<String>> hit = engine.nearestVisible(eye, new Vec3(1, 0, 0), 10.0, List.of(near));
        assertEquals("near", hit.orElseThrow().hitbox().target());
    }

    @Test
    void aWallHidesTheHitbox() {
        world.setBlock(new BlockPos(1, 0, 0), BlockState.SOLID);
        assertTrue(engine.nearestVisible(eye, new Vec3(1, 0, 0), 10.0, List.of(near)).isEmpty());
    }
}
