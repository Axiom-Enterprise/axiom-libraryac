package com.github.axiom.ac.math;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed-capacity FIFO ring buffer. Once full, adding a new element
 * evicts the oldest. Index 0 is always the oldest retained element.
 *
 * <p>Not thread-safe — intended for per-player, thread-confined use.
 *
 * @param <T> element type
 */
public final class RollingBuffer<T> {

    private final Object[] data;
    private int head;
    private int size;

    public RollingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.data = new Object[capacity];
    }

    /** Appends {@code value}, evicting the oldest element when full. */
    public void add(T value) {
        if (size < data.length) {
            data[(head + size) % data.length] = value;
            size++;
        } else {
            data[head] = value;
            head = (head + 1) % data.length;
        }
    }

    /** Returns the element at {@code index}, where 0 is the oldest. */
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index: " + index + ", size: " + size);
        }
        return (T) data[(head + index) % data.length];
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return data.length;
    }

    public boolean isFull() {
        return size == data.length;
    }

    /** Returns retained elements, oldest first. */
    public List<T> toList() {
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(get(i));
        }
        return list;
    }
}
