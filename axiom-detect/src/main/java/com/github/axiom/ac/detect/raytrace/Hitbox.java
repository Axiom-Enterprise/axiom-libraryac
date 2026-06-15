package com.github.axiom.ac.detect.raytrace;

import com.github.axiom.ac.math.Aabb;
import java.util.Objects;

/**
 * A raytrace target: a bounding box paired with the caller's handle
 * for whatever it represents (an entity id, a UUID, the entity
 * object). The handle is opaque to the engine and travels back out on
 * the {@link RayHit} so the caller can identify what was struck.
 *
 * @param target opaque caller-defined identifier
 * @param box    the box to test against
 * @param <T>    handle type
 */
public record Hitbox<T>(T target, Aabb box) {

    public Hitbox {
        Objects.requireNonNull(box, "box");
    }
}
