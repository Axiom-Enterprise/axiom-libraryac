package com.github.axiom.ac.detect.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionStoreTest {

    private final SessionStore<int[]> store = new SessionStore<>();
    private final UUID id = UUID.randomUUID();

    @Test
    void getOrCreateBuildsOnceThenReuses() {
        int[] first = store.getOrCreate(id, () -> new int[] {1});
        int[] second = store.getOrCreate(id, () -> new int[] {2});
        assertSame(first, second);
    }

    @Test
    void getReflectsPutAndRemove() {
        assertTrue(store.get(id).isEmpty());
        store.getOrCreate(id, () -> new int[] {7});
        assertEquals(7, store.get(id).orElseThrow()[0]);
        store.remove(id);
        assertTrue(store.get(id).isEmpty());
    }

    @Test
    void putReplacesExisting() {
        store.getOrCreate(id, () -> new int[] {1});
        store.put(id, new int[] {9});
        assertEquals(9, store.get(id).orElseThrow()[0]);
    }

    @Test
    void sizeAndClear() {
        store.getOrCreate(UUID.randomUUID(), () -> new int[0]);
        store.getOrCreate(UUID.randomUUID(), () -> new int[0]);
        assertEquals(2, store.size());
        store.clear();
        assertEquals(0, store.size());
    }

    @Test
    void rejectsNulls() {
        assertThrows(NullPointerException.class, () -> store.getOrCreate(null, () -> new int[0]));
        assertThrows(NullPointerException.class, () -> store.getOrCreate(id, null));
        assertThrows(NullPointerException.class, () -> store.put(null, new int[0]));
        assertThrows(NullPointerException.class, () -> store.put(id, null));
    }
}
