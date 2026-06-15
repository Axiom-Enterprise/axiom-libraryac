package com.github.axiom.ac.detect.raytrace;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.Ray;
import com.github.axiom.ac.math.Vec3;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Measures how far a player reached to strike a target — the distance
 * from the eye to the target box along the look direction. A reach
 * check flags when this exceeds the server's interaction range.
 *
 * <p>Two refinements live here. Expand the target box by a tolerance
 * before measuring to absorb the client/server hitbox mismatch (the
 * caller does this with {@link Aabb#expand}). And, because latency
 * makes the exact eye position uncertain, sweep over the candidate
 * eye positions for the tick and take the smallest reach — flagging
 * only when even the most forgiving position is out of range.
 */
public final class ReachResolver {

    private ReachResolver() {
    }

    /**
     * Distance from {@code eye} to {@code target} along
     * {@code direction}, or empty when the look ray misses the box.
     * {@code direction} need not be normalised.
     */
    public static OptionalDouble distance(Vec3 eye, Vec3 direction, Aabb target) {
        Objects.requireNonNull(eye, "eye");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(target, "target");
        // A unit direction makes the slab parameter t a world distance.
        return new Ray(eye, direction.normalize()).intersect(target);
    }

    /**
     * Smallest {@link #distance} across {@code eyePositions} — the
     * latency-compensated reach. Empty when the look ray misses the
     * box from every candidate eye position.
     */
    public static OptionalDouble minimumDistance(Iterable<Vec3> eyePositions,
                                                 Vec3 direction, Aabb target) {
        Objects.requireNonNull(eyePositions, "eyePositions");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(target, "target");
        Vec3 unit = direction.normalize();
        double best = Double.POSITIVE_INFINITY;
        boolean hit = false;
        for (Vec3 eye : eyePositions) {
            Objects.requireNonNull(eye, "eye");
            OptionalDouble reach = new Ray(eye, unit).intersect(target);
            if (reach.isPresent() && reach.getAsDouble() < best) {
                best = reach.getAsDouble();
                hit = true;
            }
        }
        return hit ? OptionalDouble.of(best) : OptionalDouble.empty();
    }
}
