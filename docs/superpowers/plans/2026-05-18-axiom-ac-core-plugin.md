# Axiom AC — Core & Plugin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `axiom-core` (the runtime that wires every module together: storage providers, the check registry, the central runtime, the singleton holder) and `axiom-plugin` (the thin Paper plugin that bootstraps it).

**Architecture:** `axiom-core` depends on all four prior modules plus Gson; it is fully unit-tested. `axiom-plugin` is a thin Paper `JavaPlugin` — pure glue against Paper API and PacketEvents, both `compileOnly`, verified by compilation.

**Tech Stack:** Java 21, Gradle (multi-module), JUnit 6, Gson 2.11.0, Paper API 1.21.x (`compileOnly`), PacketEvents 2.12.1 (`compileOnly`).

This plan is **Plan 5 of 5**. It implements `axiom-core` and `axiom-plugin` from `docs/superpowers/specs/2026-05-18-axiom-ac-design.md` (sections 4.5 and 3). Plans 1-4 are merged.

---

## IMPORTANT: Paper/PacketEvents glue (Tasks 8-9)

Tasks 1-6 and 10 are fully specified verbatim code. **Tasks 8-9 (`AxiomPlugin`, `plugin.yml`) are best-effort glue.** Paper API and PacketEvents class names are written from the known API but MUST be verified by compiling against the real dependencies (`.\gradlew.bat :axiom-plugin:compileJava`). Correct any API mismatch — wrong class, renamed method, changed signature, or an unresolvable Paper API version — until it compiles. The *structure* of `AxiomPlugin` is fixed; only API names and the Paper version may be adjusted. Do not change `axiom-core` to accommodate the glue.

---

## File Structure

```
axiom-libraryac/
├── settings.gradle                         MODIFY — add axiom-core, axiom-plugin
├── axiom-core/
│   ├── build.gradle                        CREATE — all 4 modules + Gson
│   └── src/
│       ├── main/java/com/github/axiom/ac/core/
│       │   ├── MemoryStorageProvider.java   In-memory StorageProvider
│       │   ├── JsonStorageProvider.java     JSON-file StorageProvider
│       │   ├── CheckRegistry.java           Check registration + fault isolation
│       │   ├── AxiomProvider.java           Static runtime singleton holder
│       │   └── AxiomRuntime.java            Central runtime wiring
│       └── test/java/com/github/axiom/ac/core/
│           └── (test classes per the tasks below)
└── axiom-plugin/
    ├── build.gradle                         CREATE — core+packet, Paper/PE compileOnly
    └── src/main/
        ├── java/com/github/axiom/ac/plugin/
        │   └── AxiomPlugin.java             Paper JavaPlugin bootstrap
        └── resources/
            └── plugin.yml                   Paper plugin descriptor
```

**Windows note:** commands use `.\gradlew.bat` (PowerShell).

---

## Task 1: Add the axiom-core module

**Files:**
- Modify: `settings.gradle`
- Create: `axiom-core/build.gradle`

- [ ] **Step 1: Replace `settings.gradle`**

```groovy
rootProject.name = 'axiom-libraryac'

include 'axiom-math'
include 'axiom-api'
include 'axiom-packet'
include 'axiom-world'
include 'axiom-core'
include 'axiom-plugin'
```

- [ ] **Step 2: Create `axiom-core/build.gradle`**

```groovy
// axiom-core: the runtime that wires every module together —
// storage providers, the check registry, and the central runtime.
// Fully unit-tested; depends on all four prior modules and Gson.
dependencies {
    implementation project(':axiom-api')
    implementation project(':axiom-math')
    implementation project(':axiom-packet')
    implementation project(':axiom-world')
    implementation 'com.google.code.gson:gson:2.11.0'
}
```

- [ ] **Step 3: Verify the module is recognised**

Run: `.\gradlew.bat projects`
Expected: output lists `Project ':axiom-core'` and `Project ':axiom-plugin'`.

(`axiom-plugin` will report no build file yet — that is fine; it is created in Task 7.)

- [ ] **Step 4: Commit**

```bash
git add settings.gradle axiom-core/build.gradle
git commit -m "build: add axiom-core module"
```

---

## Task 2: MemoryStorageProvider — in-memory storage

**Files:**
- Create: `axiom-core/src/main/java/com/github/axiom/ac/core/MemoryStorageProvider.java`
- Test: `axiom-core/src/test/java/com/github/axiom/ac/core/MemoryStorageProviderTest.java`

The default `StorageProvider`: keeps violations in memory, nothing is persisted.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.api.Violation;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MemoryStorageProviderTest {

    private final MemoryStorageProvider storage = new MemoryStorageProvider();

    @Test
    void unknownPlayerHasNoViolations() {
        assertTrue(storage.loadViolations(UUID.randomUUID()).isEmpty());
    }

    @Test
    void savedViolationsAreLoadedBackInOrder() {
        UUID player = UUID.randomUUID();
        Violation first = new Violation("speed", "a", 1.0, 0.5);
        Violation second = new Violation("reach", "b", 2.0, 0.9);

        storage.saveViolation(player, first);
        storage.saveViolation(player, second);

        assertEquals(List.of(first, second), storage.loadViolations(player));
    }

    @Test
    void violationsAreKeyedPerPlayer() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        storage.saveViolation(a, new Violation("speed", "a", 1.0, 0.5));

        assertEquals(1, storage.loadViolations(a).size());
        assertTrue(storage.loadViolations(b).isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-core:test --tests "com.github.axiom.ac.core.MemoryStorageProviderTest"`
Expected: FAIL — compilation error, `MemoryStorageProvider` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.core;

import com.github.axiom.ac.api.StorageProvider;
import com.github.axiom.ac.api.Violation;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory {@link StorageProvider}. Violations live only for the
 * lifetime of the process — nothing is persisted. This is the
 * default provider.
 */
public final class MemoryStorageProvider implements StorageProvider {

    private final Map<UUID, List<Violation>> byPlayer = new ConcurrentHashMap<>();

    @Override
    public void saveViolation(UUID playerId, Violation violation) {
        byPlayer.computeIfAbsent(playerId, key -> new CopyOnWriteArrayList<>())
                .add(violation);
    }

    @Override
    public List<Violation> loadViolations(UUID playerId) {
        return List.copyOf(byPlayer.getOrDefault(playerId, List.of()));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-core:test --tests "com.github.axiom.ac.core.MemoryStorageProviderTest"`
Expected: PASS — 3 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-core/src/main/java/com/github/axiom/ac/core/MemoryStorageProvider.java axiom-core/src/test/java/com/github/axiom/ac/core/MemoryStorageProviderTest.java
git commit -m "feat: add MemoryStorageProvider to axiom-core"
```

---

## Task 3: JsonStorageProvider — JSON-file storage

**Files:**
- Create: `axiom-core/src/main/java/com/github/axiom/ac/core/JsonStorageProvider.java`
- Test: `axiom-core/src/test/java/com/github/axiom/ac/core/JsonStorageProviderTest.java`

A `StorageProvider` backed by a JSON file. State is held in memory and rewritten to the file on every save; the file is read back on construction, so a fresh provider over the same path recovers prior violations. Uses Gson. An I/O failure surfaces as an unchecked `UncheckedIOException`.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.api.Violation;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonStorageProviderTest {

    @Test
    void unknownPlayerHasNoViolations(@TempDir Path dir) {
        JsonStorageProvider storage = new JsonStorageProvider(dir.resolve("v.json"));
        assertTrue(storage.loadViolations(UUID.randomUUID()).isEmpty());
    }

    @Test
    void savedViolationIsLoadedBackInSameInstance(@TempDir Path dir) {
        JsonStorageProvider storage = new JsonStorageProvider(dir.resolve("v.json"));
        UUID player = UUID.randomUUID();
        Violation violation = new Violation("speed", "too fast", 1.5, 0.8);

        storage.saveViolation(player, violation);

        assertEquals(java.util.List.of(violation), storage.loadViolations(player));
    }

    @Test
    void violationsPersistAcrossInstances(@TempDir Path dir) {
        Path file = dir.resolve("v.json");
        UUID player = UUID.randomUUID();
        Violation violation = new Violation("reach", "too far", 4.2, 0.95);

        new JsonStorageProvider(file).saveViolation(player, violation);
        JsonStorageProvider reopened = new JsonStorageProvider(file);

        assertEquals(java.util.List.of(violation), reopened.loadViolations(player));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-core:test --tests "com.github.axiom.ac.core.JsonStorageProviderTest"`
Expected: FAIL — compilation error, `JsonStorageProvider` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-core:test --tests "com.github.axiom.ac.core.JsonStorageProviderTest"`
Expected: PASS — 3 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-core/src/main/java/com/github/axiom/ac/core/JsonStorageProvider.java axiom-core/src/test/java/com/github/axiom/ac/core/JsonStorageProviderTest.java
git commit -m "feat: add JsonStorageProvider to axiom-core"
```

---

## Task 4: CheckRegistry — check registration and fault isolation

**Files:**
- Create: `axiom-core/src/main/java/com/github/axiom/ac/core/CheckRegistry.java`
- Test: `axiom-core/src/test/java/com/github/axiom/ac/core/CheckRegistryTest.java`

Holds the registered `Check`s and runs them all against a `PlayerData`. Each check is invoked inside a try/catch — a throwing check never breaks the others. Consecutive faults are counted per check; a successful inspection resets the count. When a check reaches the fault threshold it is removed and a `CheckFaultEvent` is published on the bus.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.api.Check;
import com.github.axiom.ac.api.EventBus;
import com.github.axiom.ac.api.PlayerData;
import com.github.axiom.ac.api.Violation;
import com.github.axiom.ac.api.event.CheckFaultEvent;
import com.github.axiom.ac.math.Vec3;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CheckRegistryTest {

    private record FakePlayerData(UUID uuid, Vec3 position, Vec3 velocity,
                                  float yaw, float pitch, boolean onGround)
            implements PlayerData {
    }

    private static PlayerData player() {
        return new FakePlayerData(UUID.randomUUID(), new Vec3(0, 0, 0),
                new Vec3(0, 0, 0), 0.0f, 0.0f, true);
    }

    /** Always flags. */
    private static Check flagging(String id) {
        return new Check() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Optional<Violation> inspect(PlayerData data) {
                return Optional.of(new Violation(id, "flagged", 1.0, 1.0));
            }
        };
    }

    /** Always throws. */
    private static Check throwing(String id) {
        return new Check() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Optional<Violation> inspect(PlayerData data) {
                throw new IllegalStateException("boom");
            }
        };
    }

    @Test
    void inspectCollectsViolationsFromEveryCheck() {
        CheckRegistry registry = new CheckRegistry(new EventBus());
        registry.register(flagging("speed"));
        registry.register(flagging("reach"));

        List<Violation> violations = registry.inspect(player());

        assertEquals(2, violations.size());
    }

    @Test
    void aThrowingCheckDoesNotBreakOthers() {
        CheckRegistry registry = new CheckRegistry(new EventBus());
        registry.register(throwing("bad"));
        registry.register(flagging("speed"));

        List<Violation> violations = registry.inspect(player());

        assertEquals(1, violations.size());
        assertEquals("speed", violations.get(0).checkId());
    }

    @Test
    void aCheckIsRemovedAfterReachingTheFaultThreshold() {
        EventBus bus = new EventBus();
        AtomicInteger faults = new AtomicInteger();
        bus.channel(CheckFaultEvent.class).subscribe(event -> faults.incrementAndGet());
        CheckRegistry registry = new CheckRegistry(bus, 3);
        registry.register(throwing("bad"));

        registry.inspect(player());
        registry.inspect(player());
        assertTrue(registry.isRegistered("bad"));
        assertEquals(0, faults.get());

        registry.inspect(player());

        assertFalse(registry.isRegistered("bad"));
        assertEquals(1, faults.get());
    }

    @Test
    void unregisterRemovesACheck() {
        CheckRegistry registry = new CheckRegistry(new EventBus());
        registry.register(flagging("speed"));
        registry.unregister("speed");
        assertFalse(registry.isRegistered("speed"));
        assertTrue(registry.inspect(player()).isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-core:test --tests "com.github.axiom.ac.core.CheckRegistryTest"`
Expected: FAIL — compilation error, `CheckRegistry` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.core;

import com.github.axiom.ac.api.Check;
import com.github.axiom.ac.api.EventBus;
import com.github.axiom.ac.api.PlayerData;
import com.github.axiom.ac.api.Violation;
import com.github.axiom.ac.api.event.CheckFaultEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the registered {@link Check}s and runs them against player
 * data. Each check runs inside a try/catch, so a throwing check
 * cannot break the others. Consecutive faults are counted per check;
 * a successful inspection resets the count. A check that reaches the
 * fault threshold is removed and a {@link CheckFaultEvent} is
 * published.
 */
public final class CheckRegistry {

    private static final int DEFAULT_FAULT_THRESHOLD = 3;

    private final EventBus eventBus;
    private final int faultThreshold;
    private final Map<String, Check> checks = new ConcurrentHashMap<>();
    private final Map<String, Integer> consecutiveFaults = new ConcurrentHashMap<>();

    public CheckRegistry(EventBus eventBus) {
        this(eventBus, DEFAULT_FAULT_THRESHOLD);
    }

    public CheckRegistry(EventBus eventBus, int faultThreshold) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        if (faultThreshold < 1) {
            throw new IllegalArgumentException("faultThreshold must be >= 1");
        }
        this.faultThreshold = faultThreshold;
    }

    /** Registers {@code check}, keyed by its id. */
    public void register(Check check) {
        checks.put(check.id(), check);
    }

    /** Removes the check with the given id, if present. */
    public void unregister(String id) {
        checks.remove(id);
        consecutiveFaults.remove(id);
    }

    /** True when a check with {@code id} is currently registered. */
    public boolean isRegistered(String id) {
        return checks.containsKey(id);
    }

    /** Number of currently registered checks. */
    public int size() {
        return checks.size();
    }

    /**
     * Runs every registered check against {@code data} and returns
     * the violations they produced. A check that throws is isolated;
     * one that reaches the fault threshold is removed.
     */
    public List<Violation> inspect(PlayerData data) {
        List<Violation> violations = new ArrayList<>();
        for (Check check : checks.values()) {
            try {
                check.inspect(data).ifPresent(violations::add);
                consecutiveFaults.remove(check.id());
            } catch (RuntimeException fault) {
                recordFault(check.id());
            }
        }
        return violations;
    }

    private void recordFault(String checkId) {
        int faults = consecutiveFaults.merge(checkId, 1, Integer::sum);
        if (faults >= faultThreshold) {
            checks.remove(checkId);
            consecutiveFaults.remove(checkId);
            eventBus.publish(new CheckFaultEvent(checkId,
                    faults + " consecutive faults"));
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-core:test --tests "com.github.axiom.ac.core.CheckRegistryTest"`
Expected: PASS — 4 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-core/src/main/java/com/github/axiom/ac/core/CheckRegistry.java axiom-core/src/test/java/com/github/axiom/ac/core/CheckRegistryTest.java
git commit -m "feat: add CheckRegistry with fault isolation to axiom-core"
```

---

## Task 5: AxiomRuntime — central runtime wiring

**Files:**
- Create: `axiom-core/src/main/java/com/github/axiom/ac/core/AxiomRuntime.java`
- Test: `axiom-core/src/test/java/com/github/axiom/ac/core/AxiomRuntimeTest.java`

The central object that wires every module together. It owns the `EventBus`, the `PlayerRegistry`, the `WorldCache`/`CollisionEngine`, the `CheckRegistry`, and the `StorageProvider`. It handles player lifecycle (registering/unregistering players and publishing the lifecycle events) and an inspection pass (running checks for a player, publishing a `FlagEvent` per violation, and persisting non-cancelled violations).

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.api.Check;
import com.github.axiom.ac.api.PlayerData;
import com.github.axiom.ac.api.Violation;
import com.github.axiom.ac.api.event.FlagEvent;
import com.github.axiom.ac.api.event.PlayerJoinEvent;
import com.github.axiom.ac.api.event.PlayerQuitEvent;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AxiomRuntimeTest {

    private static Check flagging(String id) {
        return new Check() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Optional<Violation> inspect(PlayerData data) {
                return Optional.of(new Violation(id, "flagged", 1.0, 1.0));
            }
        };
    }

    @Test
    void playerJoinRegistersAndPublishes() {
        AxiomRuntime runtime = new AxiomRuntime(new MemoryStorageProvider());
        AtomicInteger joins = new AtomicInteger();
        runtime.eventBus().channel(PlayerJoinEvent.class).subscribe(e -> joins.incrementAndGet());
        UUID player = UUID.randomUUID();

        runtime.handlePlayerJoin(player);

        assertEquals(1, joins.get());
        assertTrue(runtime.players().get(player).isPresent());
    }

    @Test
    void playerQuitUnregistersAndPublishes() {
        AxiomRuntime runtime = new AxiomRuntime(new MemoryStorageProvider());
        AtomicInteger quits = new AtomicInteger();
        runtime.eventBus().channel(PlayerQuitEvent.class).subscribe(e -> quits.incrementAndGet());
        UUID player = UUID.randomUUID();
        runtime.handlePlayerJoin(player);

        runtime.handlePlayerQuit(player);

        assertEquals(1, quits.get());
        assertFalse(runtime.players().get(player).isPresent());
    }

    @Test
    void inspectPublishesFlagEventAndPersistsViolation() {
        MemoryStorageProvider storage = new MemoryStorageProvider();
        AxiomRuntime runtime = new AxiomRuntime(storage);
        runtime.checks().register(flagging("speed"));
        AtomicInteger flags = new AtomicInteger();
        runtime.eventBus().channel(FlagEvent.class).subscribe(e -> flags.incrementAndGet());
        UUID player = UUID.randomUUID();
        runtime.handlePlayerJoin(player);

        runtime.inspect(player);

        assertEquals(1, flags.get());
        assertEquals(1, storage.loadViolations(player).size());
    }

    @Test
    void cancelledFlagEventIsNotPersisted() {
        MemoryStorageProvider storage = new MemoryStorageProvider();
        AxiomRuntime runtime = new AxiomRuntime(storage);
        runtime.checks().register(flagging("speed"));
        runtime.eventBus().channel(FlagEvent.class).subscribe(e -> e.setCancelled(true));
        UUID player = UUID.randomUUID();
        runtime.handlePlayerJoin(player);

        runtime.inspect(player);

        assertTrue(storage.loadViolations(player).isEmpty());
    }

    @Test
    void inspectingAnUnknownPlayerDoesNothing() {
        AxiomRuntime runtime = new AxiomRuntime(new MemoryStorageProvider());
        runtime.checks().register(flagging("speed"));
        // No join — player is unknown.
        runtime.inspect(UUID.randomUUID());
        // No exception, nothing to assert beyond reaching here.
        assertEquals(0, runtime.players().size());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-core:test --tests "com.github.axiom.ac.core.AxiomRuntimeTest"`
Expected: FAIL — compilation error, `AxiomRuntime` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.core;

import com.github.axiom.ac.api.EventBus;
import com.github.axiom.ac.api.StorageProvider;
import com.github.axiom.ac.api.Violation;
import com.github.axiom.ac.api.event.FlagEvent;
import com.github.axiom.ac.api.event.PlayerJoinEvent;
import com.github.axiom.ac.api.event.PlayerQuitEvent;
import com.github.axiom.ac.packet.PlayerDataImpl;
import com.github.axiom.ac.packet.PlayerRegistry;
import com.github.axiom.ac.world.CollisionEngine;
import com.github.axiom.ac.world.WorldCache;
import java.util.Objects;
import java.util.UUID;

/**
 * Central runtime: wires together the event bus, the player
 * registry, the world model, the check registry and storage. Handles
 * player lifecycle and inspection passes.
 */
public final class AxiomRuntime {

    private final EventBus eventBus = new EventBus();
    private final PlayerRegistry players = new PlayerRegistry();
    private final WorldCache world = new WorldCache();
    private final CollisionEngine collision = new CollisionEngine(world);
    private final CheckRegistry checks = new CheckRegistry(eventBus);
    private final StorageProvider storage;

    public AxiomRuntime(StorageProvider storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    /** The event bus other plugins subscribe to. */
    public EventBus eventBus() {
        return eventBus;
    }

    /** The registry of tracked players. */
    public PlayerRegistry players() {
        return players;
    }

    /** The server-side world block cache. */
    public WorldCache world() {
        return world;
    }

    /** The collision engine over {@link #world()}. */
    public CollisionEngine collision() {
        return collision;
    }

    /** The registry of detection checks. */
    public CheckRegistry checks() {
        return checks;
    }

    /** The configured storage provider. */
    public StorageProvider storage() {
        return storage;
    }

    /** Starts tracking a player and publishes a {@link PlayerJoinEvent}. */
    public void handlePlayerJoin(UUID playerId) {
        players.register(playerId);
        eventBus.publish(new PlayerJoinEvent(playerId));
    }

    /** Stops tracking a player and publishes a {@link PlayerQuitEvent}. */
    public void handlePlayerQuit(UUID playerId) {
        players.unregister(playerId);
        eventBus.publish(new PlayerQuitEvent(playerId));
    }

    /**
     * Runs every registered check for the given player. Each
     * violation is published as a {@link FlagEvent}; a violation
     * whose event was not cancelled is persisted. Unknown players are
     * ignored.
     */
    public void inspect(UUID playerId) {
        PlayerDataImpl data = players.get(playerId).orElse(null);
        if (data == null) {
            return;
        }
        for (Violation violation : checks.inspect(data)) {
            FlagEvent event = new FlagEvent(playerId, violation);
            eventBus.publish(event);
            if (!event.isCancelled()) {
                storage.saveViolation(playerId, violation);
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-core:test --tests "com.github.axiom.ac.core.AxiomRuntimeTest"`
Expected: PASS — 5 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-core/src/main/java/com/github/axiom/ac/core/AxiomRuntime.java axiom-core/src/test/java/com/github/axiom/ac/core/AxiomRuntimeTest.java
git commit -m "feat: add AxiomRuntime central wiring to axiom-core"
```

---

## Task 6: AxiomProvider — runtime singleton holder

**Files:**
- Create: `axiom-core/src/main/java/com/github/axiom/ac/core/AxiomProvider.java`
- Test: `axiom-core/src/test/java/com/github/axiom/ac/core/AxiomProviderTest.java`

A static holder for the shared `AxiomRuntime` (created in Task 5), so plugins integrating with a running Axiom instance can reach it. Before the runtime is set, `get()` is empty.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.core;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AxiomProviderTest {

    @AfterEach
    void reset() {
        AxiomProvider.clear();
    }

    @Test
    void getIsEmptyBeforeARuntimeIsSet() {
        assertTrue(AxiomProvider.get().isEmpty());
    }

    @Test
    void getReturnsTheSetRuntime() {
        AxiomRuntime runtime = new AxiomRuntime(new MemoryStorageProvider());
        AxiomProvider.set(runtime);
        assertSame(runtime, AxiomProvider.get().orElseThrow());
    }

    @Test
    void clearRemovesTheRuntime() {
        AxiomProvider.set(new AxiomRuntime(new MemoryStorageProvider()));
        AxiomProvider.clear();
        assertTrue(AxiomProvider.get().isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-core:test --tests "com.github.axiom.ac.core.AxiomProviderTest"`
Expected: FAIL — compilation error, `AxiomProvider` does not exist. (`AxiomRuntime` and `MemoryStorageProvider` already exist from earlier tasks.)

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.core;

import java.util.Objects;
import java.util.Optional;

/**
 * Static holder for the shared {@link AxiomRuntime}. The plugin sets
 * the runtime on enable and clears it on disable; integrating
 * plugins read it through {@link #get()}.
 */
public final class AxiomProvider {

    private static volatile AxiomRuntime runtime;

    private AxiomProvider() {
    }

    /** Sets the shared runtime instance. */
    public static void set(AxiomRuntime runtime) {
        AxiomProvider.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    /** The shared runtime, or empty when none has been set. */
    public static Optional<AxiomRuntime> get() {
        return Optional.ofNullable(runtime);
    }

    /** Clears the shared runtime. */
    public static void clear() {
        runtime = null;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-core:test --tests "com.github.axiom.ac.core.AxiomProviderTest"`
Expected: PASS — 3 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-core/src/main/java/com/github/axiom/ac/core/AxiomProvider.java axiom-core/src/test/java/com/github/axiom/ac/core/AxiomProviderTest.java
git commit -m "feat: add AxiomProvider runtime holder to axiom-core"
```

---

## Task 7: Add the axiom-plugin module

**Files:**
- Create: `axiom-plugin/build.gradle`

(`settings.gradle` already includes `axiom-plugin` from Task 1.)

- [ ] **Step 1: Create `axiom-plugin/build.gradle`**

```groovy
// axiom-plugin: the thin Paper plugin that bootstraps axiom-core.
// Pure glue — Paper API and PacketEvents are compileOnly (provided
// by the server at runtime).
repositories {
    maven { url = 'https://repo.papermc.io/repository/maven-public/' }
    maven { url = 'https://repo.codemc.io/repository/maven-releases/' }
}

dependencies {
    implementation project(':axiom-core')
    implementation project(':axiom-packet')
    compileOnly 'io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT'
    compileOnly 'com.github.retrooper:packetevents-spigot:2.12.1'
}
```

- [ ] **Step 2: Verify the module resolves**

Run: `.\gradlew.bat :axiom-plugin:dependencies --configuration compileClasspath`
Expected: `BUILD SUCCESSFUL`; the Paper API and PacketEvents artifacts resolve. If `paper-api:1.21.4-R0.1-SNAPSHOT` does not resolve, change the version to the latest available `1.21.x-R0.1-SNAPSHOT` in the Paper repository and retry.

- [ ] **Step 3: Commit**

```bash
git add axiom-plugin/build.gradle
git commit -m "build: add axiom-plugin module with Paper and PacketEvents"
```

---

## Task 8: AxiomPlugin — Paper plugin bootstrap

**Files:**
- Create: `axiom-plugin/src/main/java/com/github/axiom/ac/plugin/AxiomPlugin.java`

**Best-effort glue — see the "IMPORTANT" section at the top of this plan.** Verified by `:axiom-plugin:compileJava`.

`AxiomPlugin` is the Paper `JavaPlugin` entry point. `onLoad` builds and loads PacketEvents; `onEnable` initialises PacketEvents, creates the `AxiomRuntime` with in-memory storage, publishes it via `AxiomProvider`, registers the `PacketPipeline` listener and the Bukkit join/quit listener; `onDisable` clears the provider and terminates PacketEvents.

- [ ] **Step 1: Write the implementation (best-effort)**

```java
package com.github.axiom.ac.plugin;

import com.github.axiom.ac.core.AxiomProvider;
import com.github.axiom.ac.core.AxiomRuntime;
import com.github.axiom.ac.core.MemoryStorageProvider;
import com.github.axiom.ac.packet.PacketPipeline;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper plugin entry point. Bootstraps {@link AxiomRuntime} as a
 * shared singleton, wires the PacketEvents pipeline, and bridges
 * Bukkit player join/quit into the runtime.
 */
public final class AxiomPlugin extends JavaPlugin implements Listener {

    private AxiomRuntime runtime;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().init();

        runtime = new AxiomRuntime(new MemoryStorageProvider());
        AxiomProvider.set(runtime);

        PacketEvents.getAPI().getEventManager()
                .registerListener(new PacketPipeline(runtime.players()));
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("Axiom AC enabled.");
    }

    @Override
    public void onDisable() {
        AxiomProvider.clear();
        if (PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().terminate();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        runtime.handlePlayerJoin(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        runtime.handlePlayerQuit(event.getPlayer().getUniqueId());
    }
}
```

- [ ] **Step 2: Compile and fix any API mismatch**

Run: `.\gradlew.bat :axiom-plugin:compileJava`

If compilation fails on a Paper or PacketEvents name, consult the real APIs and correct. Things to verify:
- `JavaPlugin`, `Listener`, `EventHandler`, `PlayerJoinEvent`/`PlayerQuitEvent` packages (Bukkit/Paper).
- `PacketEvents.setAPI` / `getAPI` / `SpigotPacketEventsBuilder.build` / `load()` / `init()` / `terminate()`.
- `getEventManager().registerListener(...)` signature — the `PacketPipeline` (a `PacketListenerAbstract`) may be passed directly, or may need a priority argument; adjust to whatever the 2.12.1 API accepts.

Note `PacketPipeline` already extends `PacketListenerAbstract` with `PacketListenerPriority.NORMAL` baked into its constructor (from Plan 3), so `registerListener(new PacketPipeline(...))` should suffice.

Repeat until `BUILD SUCCESSFUL`. Do not modify `axiom-core` or `axiom-packet`.

- [ ] **Step 3: Commit**

```bash
git add axiom-plugin/src/main/java/com/github/axiom/ac/plugin/AxiomPlugin.java
git commit -m "feat: add AxiomPlugin Paper bootstrap to axiom-plugin"
```

If API corrections were needed, note them in the commit body.

---

## Task 9: plugin.yml — Paper plugin descriptor

**Files:**
- Create: `axiom-plugin/src/main/resources/plugin.yml`

- [ ] **Step 1: Create `plugin.yml`**

```yaml
name: AxiomAC
version: 1.0-SNAPSHOT
main: com.github.axiom.ac.plugin.AxiomPlugin
api-version: '1.21'
author: Carlo Maria Cardí
description: Axiom AC anticheat toolkit runtime.
depend:
  - packetevents
```

- [ ] **Step 2: Verify the module builds**

Run: `.\gradlew.bat :axiom-plugin:build`
Expected: `BUILD SUCCESSFUL`; `axiom-plugin/build/libs/axiom-plugin-1.0-SNAPSHOT.jar` is produced and contains `plugin.yml`.

- [ ] **Step 3: Commit**

```bash
git add axiom-plugin/src/main/resources/plugin.yml
git commit -m "feat: add plugin.yml descriptor to axiom-plugin"
```

---

## Task 10: Full project verification

**Files:** none — verification only.

- [ ] **Step 1: Run the axiom-core test suite**

Run: `.\gradlew.bat :axiom-core:test`
Expected: PASS — `BUILD SUCCESSFUL`, 5 test classes green (18 tests total).

- [ ] **Step 2: Build the whole project**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL` — all six modules (`axiom-math`, `axiom-api`, `axiom-packet`, `axiom-world`, `axiom-core`, `axiom-plugin`) build and test green.

- [ ] **Step 3: Commit if anything changed**

If steps 1-2 produced no source changes, skip. Otherwise commit with message `build: verify axiom-core and axiom-plugin build and tests pass`.

---

## Plan Complete

`axiom-core` provides the runtime: `MemoryStorageProvider` and `JsonStorageProvider` (storage), `CheckRegistry` (check registration with fault isolation), `AxiomRuntime` (central wiring and player lifecycle), and `AxiomProvider` (the shared-runtime holder). `axiom-plugin` is the Paper plugin that bootstraps it all.

This completes the **Axiom AC toolkit** — all six modules of the design spec are implemented: the math toolkit, the public API, the packet pipeline, the world model, the runtime, and the plugin. The Phase 2 `axiom-predict` deterministic prediction engine remains as future work, as scoped in the design spec.
