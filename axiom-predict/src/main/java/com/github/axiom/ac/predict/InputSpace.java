package com.github.axiom.ac.predict;

import java.util.ArrayList;
import java.util.List;

/**
 * The full set of {@link MovementInput}s a client could have pressed
 * in one tick. The prediction search evaluates every entry.
 */
public final class InputSpace {

    private static final int[] AXES = {-1, 0, 1};
    private static final boolean[] FLAGS = {false, true};

    private static final List<MovementInput> ALL = enumerate();

    private InputSpace() {
    }

    private static List<MovementInput> enumerate() {
        List<MovementInput> inputs = new ArrayList<>();
        for (int forward : AXES) {
            for (int strafe : AXES) {
                for (boolean jump : FLAGS) {
                    for (boolean sprint : FLAGS) {
                        for (boolean sneak : FLAGS) {
                            inputs.add(new MovementInput(forward, strafe,
                                    jump, sprint, sneak));
                        }
                    }
                }
            }
        }
        return List.copyOf(inputs);
    }

    /** An immutable list of every candidate input. */
    public static List<MovementInput> all() {
        return ALL;
    }
}
