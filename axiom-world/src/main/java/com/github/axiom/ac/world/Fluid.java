package com.github.axiom.ac.world;

/**
 * The fluid a block contributes to its cell. A fluid block is
 * passable — it carries no collision shape — but it changes the
 * physics of a player whose hitbox overlaps it.
 */
public enum Fluid {

    /** No fluid: an ordinary block or air. */
    NONE,

    /** Water: buoyant, with strong drag and slow vertical motion. */
    WATER,

    /** Lava: very strong drag and very slow vertical motion. */
    LAVA
}
