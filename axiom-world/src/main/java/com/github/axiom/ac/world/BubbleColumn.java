package com.github.axiom.ac.world;

/**
 * The bubble column a water block carries, if any. A bubble column
 * forms in water above soul sand (upward) or magma (downward) and
 * drives a strong vertical current on a player submerged in it.
 */
public enum BubbleColumn {

    /** No bubble column: ordinary water or a non-water block. */
    NONE,

    /** An upward column, above soul sand, that lifts the player. */
    UPWARD,

    /** A downward column, above magma, that drags the player down. */
    DOWNWARD
}
