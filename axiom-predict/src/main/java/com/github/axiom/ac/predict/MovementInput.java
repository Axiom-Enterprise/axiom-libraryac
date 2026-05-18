package com.github.axiom.ac.predict;

/**
 * One candidate set of client movement inputs for a single tick.
 *
 * @param forward forward axis: {@code -1} (back), {@code 0}, {@code +1}
 * @param strafe  strafe axis: {@code -1}, {@code 0}, {@code +1}
 * @param jump    whether the jump key was held
 * @param sprint  whether the sprint key was held
 */
public record MovementInput(int forward, int strafe, boolean jump, boolean sprint) {

    public MovementInput {
        requireAxis(forward, "forward");
        requireAxis(strafe, "strafe");
    }

    private static void requireAxis(int value, String name) {
        if (value < -1 || value > 1) {
            throw new IllegalArgumentException(name + " must be -1, 0 or 1");
        }
    }

    /** The neutral input: no movement, no jump, no sprint. */
    public static MovementInput none() {
        return new MovementInput(0, 0, false, false);
    }
}
