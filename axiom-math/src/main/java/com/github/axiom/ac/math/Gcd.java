package com.github.axiom.ac.math;

/**
 * Greatest common divisor utilities. The array form is used for
 * periodicity analysis — a large common divisor across event
 * intervals signals constant-timed automation.
 */
public final class Gcd {

    private Gcd() {
    }

    /** Euclidean GCD. Operands are treated by absolute value. */
    public static long gcd(long a, long b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            long t = b;
            b = a % b;
            a = t;
        }
        return a;
    }

    /** GCD across every element. Throws when {@code values} is empty. */
    public static long gcdOf(long[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("values must not be empty");
        }
        long result = 0;
        for (long v : values) {
            result = gcd(result, v);
        }
        return result;
    }
}
