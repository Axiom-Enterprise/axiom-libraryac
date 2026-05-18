package com.github.axiom.ac.core;

import com.github.axiom.ac.api.StorageProvider;
import com.github.axiom.ac.api.Violation;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link StorageProvider} backed by a JSON file. Violations are held
 * in memory and the whole store is rewritten on every save; the file
 * is read back on construction, so a new provider over the same path
 * recovers prior violations.
 *
 * <p>An I/O failure is rethrown as an unchecked
 * {@link UncheckedIOException}.
 */
public final class JsonStorageProvider implements StorageProvider {

    private static final Type STORE_TYPE =
            new TypeToken<Map<UUID, List<Violation>>>() { }.getType();

    private final Path file;
    private final Gson gson = new Gson();
    private final Map<UUID, List<Violation>> byPlayer = new ConcurrentHashMap<>();

    public JsonStorageProvider(Path file) {
        this.file = Objects.requireNonNull(file, "file");
        loadFromDisk();
    }

    private void loadFromDisk() {
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Map<UUID, List<Violation>> loaded = gson.fromJson(reader, STORE_TYPE);
            if (loaded != null) {
                loaded.forEach((player, violations) ->
                        byPlayer.put(player, new CopyOnWriteArrayList<>(violations)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read storage file " + file, e);
        }
    }

    private void persist() {
        try (Writer writer = Files.newBufferedWriter(file)) {
            gson.toJson(byPlayer, STORE_TYPE, writer);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to write storage file " + file, e);
        }
    }

    @Override
    public void saveViolation(UUID playerId, Violation violation) {
        byPlayer.computeIfAbsent(playerId, key -> new CopyOnWriteArrayList<>())
                .add(violation);
        persist();
    }

    @Override
    public List<Violation> loadViolations(UUID playerId) {
        return List.copyOf(byPlayer.getOrDefault(playerId, List.of()));
    }
}
