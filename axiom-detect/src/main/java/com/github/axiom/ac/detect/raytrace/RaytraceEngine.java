package com.github.axiom.ac.detect.raytrace;

import com.github.axiom.ac.math.Ray;
import com.github.axiom.ac.math.Vec3;
import com.github.axiom.ac.world.CollisionEngine;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Casts a ray against a set of entity {@link Hitbox}es and returns the
 * one it strikes first — the basis for combat reach and aim checks.
 * The world-aware variant additionally drops targets hidden behind
 * solid blocks, so a hit reflects what the player could actually see.
 *
 * <p>The geometry comes from {@link Ray#intersect(com.github.axiom.ac.math.Aabb)};
 * occlusion comes from the {@link CollisionEngine} over the cached
 * world. The engine holds no per-player state and is safe to share.
 */
public final class RaytraceEngine {

    private final CollisionEngine collision;

    public RaytraceEngine(CollisionEngine collision) {
        this.collision = Objects.requireNonNull(collision, "collision");
    }

    /**
     * Nearest hitbox struck by {@code ray} within {@code maxDistance}
     * world units, ignoring world occlusion. Empty when the ray has no
     * direction or hits nothing in range.
     */
    public <T> Optional<RayHit<T>> nearest(Ray ray, double maxDistance, Iterable<Hitbox<T>> targets) {
        Objects.requireNonNull(ray, "ray");
        Objects.requireNonNull(targets, "targets");
        double dirLength = ray.direction().length();
        if (dirLength == 0.0) {
            return Optional.empty();
        }
        RayHit<T> best = null;
        for (Hitbox<T> target : targets) {
            RayHit<T> hit = trace(ray, dirLength, target);
            if (hit != null && hit.distance() <= maxDistance
                    && (best == null || hit.distance() < best.distance())) {
                best = hit;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Nearest hitbox visible from {@code eye} along {@code direction}
     * within {@code maxDistance} — that is, the nearest one with no
     * solid block between the eye and the entry point.
     */
    public <T> Optional<RayHit<T>> nearestVisible(Vec3 eye, Vec3 direction,
                                                  double maxDistance, Iterable<Hitbox<T>> targets) {
        Objects.requireNonNull(eye, "eye");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(targets, "targets");
        double dirLength = direction.length();
        if (dirLength == 0.0) {
            return Optional.empty();
        }
        Ray ray = new Ray(eye, direction);
        RayHit<T> best = null;
        for (Hitbox<T> target : targets) {
            RayHit<T> hit = trace(ray, dirLength, target);
            if (hit == null || hit.distance() > maxDistance) {
                continue;
            }
            if (best != null && hit.distance() >= best.distance()) {
                continue;
            }
            if (LineOfSight.clear(eye, hit.point(), collision)) {
                best = hit;
            }
        }
        return Optional.ofNullable(best);
    }

    private static <T> RayHit<T> trace(Ray ray, double dirLength, Hitbox<T> target) {
        OptionalDouble parameter = ray.intersect(target.box());
        if (parameter.isEmpty()) {
            return null;
        }
        double t = parameter.getAsDouble();
        // Ray.intersect reports t in direction-length units; multiply
        // back to world units for the reported distance.
        double distance = t * dirLength;
        Vec3 point = ray.origin().add(ray.direction().scale(t));
        return new RayHit<>(target, distance, point);
    }
}
