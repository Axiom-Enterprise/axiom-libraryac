package com.github.axiom.ac.world;

/**
 * Collision state of a cached block.
 */
public enum BlockState {

    /** Full unit-cube collision. */
    SOLID,

    /** No collision — the player passes through. */
    PASSABLE,

    /** Not cached. The world is desynced here; checks must decide
     *  how to treat the uncertainty. */
    UNKNOWN
}
