package com.github.axiom.ac.math;

import java.util.OptionalDouble;

/**
 * Combat geometry: eye positions, player hitboxes, reach distance,
 * and crosshair line-of-sight. These are the primitives a reach or
 * an aim check is built from.
 *
 * <p>Reach is the distance from the attacker's eye to the nearest
 * point of the victim's hitbox — see {@link #reachDistance}. Aim is
 * whether the attacker's look ray actually crosses that hitbox — see
 * {@link #lineOfSightDistance}. A hit that is in reach but whose ray
 * never touches the hitbox is the signature of a rotation that was
 * snapped onto the target after the fact.
 */
public final class CombatMath {

    /** Eye height of a standing player, in blocks. */
    public static final double DEFAULT_EYE_HEIGHT = 1.62;

    /** Eye height of a sneaking player, in blocks. */
    public static final double SNEAKING_EYE_HEIGHT = 1.27;

    /** Width (and depth) of the standard player hitbox, in blocks. */
    public static final double PLAYER_WIDTH = 0.6;

    /** Height of the standard player hitbox, in blocks. */
    public static final double PLAYER_HEIGHT = 1.8;

    private CombatMath() {
    }

    /** Eye position for a player whose feet are at {@code feet}. */
    public static Vec3 eyePosition(Vec3 feet, double eyeHeight) {
        return new Vec3(feet.x(), feet.y() + eyeHeight, feet.z());
    }

    /** Eye position of a standing player at {@code feet}. */
    public static Vec3 eyePosition(Vec3 feet) {
        return eyePosition(feet, DEFAULT_EYE_HEIGHT);
    }

    /**
     * The hitbox of a player at {@code feet}, centred horizontally on
     * the feet position and rising to {@code height}.
     */
    public static Aabb hitbox(Vec3 feet, double width, double height) {
        double half = width / 2.0;
        return new Aabb(
                feet.x() - half, feet.y(), feet.z() - half,
                feet.x() + half, feet.y() + height, feet.z() + half);
    }

    /** The standard player hitbox of a player at {@code feet}. */
    public static Aabb playerHitbox(Vec3 feet) {
        return hitbox(feet, PLAYER_WIDTH, PLAYER_HEIGHT);
    }

    /**
     * Distance from {@code eye} to the nearest point of
     * {@code targetHitbox} — the reach of an attack. Zero when the
     * eye is inside the hitbox.
     */
    public static double reachDistance(Vec3 eye, Aabb targetHitbox) {
        return targetHitbox.distanceTo(eye);
    }

    /** True when {@code targetHitbox} is reachable from {@code eye}. */
    public static boolean withinReach(Vec3 eye, Aabb targetHitbox, double maxReach) {
        return reachDistance(eye, targetHitbox) <= maxReach;
    }

    /**
     * Distance along the look ray from {@code eye}, rotated by
     * {@code rotation}, to the first intersection with
     * {@code targetHitbox} — or empty when the crosshair does not
     * cross the hitbox at all. A present value is the true
     * eye-to-target distance for an attack that was genuinely aimed.
     */
    public static OptionalDouble lineOfSightDistance(Vec3 eye, Rotation rotation,
                                                     Aabb targetHitbox) {
        return new Ray(eye, rotation.directionVector()).intersect(targetHitbox);
    }

    /**
     * True when the look ray from {@code eye} crosses
     * {@code targetHitbox} within {@code maxDistance} — the attacker
     * is both aimed at and in range of the target.
     */
    public static boolean looksAt(Vec3 eye, Rotation rotation, Aabb targetHitbox,
                                  double maxDistance) {
        OptionalDouble distance = lineOfSightDistance(eye, rotation, targetHitbox);
        return distance.isPresent() && distance.getAsDouble() <= maxDistance;
    }
}
