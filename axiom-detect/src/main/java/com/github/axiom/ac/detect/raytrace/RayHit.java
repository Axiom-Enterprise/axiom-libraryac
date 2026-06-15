package com.github.axiom.ac.detect.raytrace;

import com.github.axiom.ac.math.Vec3;

/**
 * A ray's intersection with a {@link Hitbox}.
 *
 * @param hitbox   the box that was struck
 * @param distance world-space distance from the ray origin to the entry point
 * @param point    the entry point in world space
 * @param <T>      hitbox handle type
 */
public record RayHit<T>(Hitbox<T> hitbox, double distance, Vec3 point) {
}
