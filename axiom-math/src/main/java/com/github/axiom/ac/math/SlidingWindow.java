package com.github.axiom.ac.math;

/**
 * Fixed-size sliding window over double samples. New samples evict
 * the oldest once the window is full. Statistics are computed over
 * the currently retained samples via {@link Stats}.
 *
 * <p>Not thread-safe — intended for per-player, thread-confined use.
 */
public final class SlidingWindow {

    private final RollingBuffer<Double> buffer;

    public SlidingWindow(int capacity) {
        this.buffer = new RollingBuffer<>(capacity);
    }

    public void add(double value) {
        buffer.add(value);
    }

    public int size() {
        return buffer.size();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public boolean isFull() {
        return buffer.isFull();
    }

    /** Retained samples, oldest first. */
    public double[] toArray() {
        double[] values = new double[buffer.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = buffer.get(i);
        }
        return values;
    }

    /** Mean of the retained samples. Throws when the window is empty. */
    public double mean() {
        return Stats.mean(toArray());
    }

    /** Population standard deviation of the retained samples. */
    public double standardDeviation() {
        return Stats.standardDeviation(toArray());
    }
}
