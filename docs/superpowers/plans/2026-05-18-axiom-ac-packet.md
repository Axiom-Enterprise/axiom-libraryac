# Axiom AC — Packet Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `axiom-packet`, the PacketEvents-backed packet pipeline: decoded movement updates, the concrete per-player data implementation, the player registry, the transaction (latency-compensation) manager, and the PacketEvents listener glue.

**Architecture:** A Gradle module depending on `axiom-api` and `axiom-math`, with PacketEvents as a `compileOnly` dependency (the server/plugin provides it at runtime). The testable core — `MovementUpdate`, `PlayerDataImpl`, `PlayerRegistry`, `TransactionManager` — is free of any PacketEvents type and is fully unit-tested. Only `PacketPipeline`, the thin `PacketListener` glue, touches PacketEvents; it is verified by compilation against the real dependency.

**Tech Stack:** Java 21, Gradle (multi-module), JUnit 6, PacketEvents 2.12.1 (`compileOnly`).

This plan is **Plan 3 of 5**. It implements `axiom-packet` from `docs/superpowers/specs/2026-05-18-axiom-ac-design.md` (section 4.3). Plans 1 (`axiom-math`) and 2 (`axiom-api`) are merged.

---

## IMPORTANT: PacketEvents glue (Task 7)

Tasks 1-6 are fully specified verbatim code — copy them exactly. **Task 7 (`PacketPipeline`) is best-effort glue against PacketEvents 2.12.1.** PacketEvents class and method names in Task 7 are written from the 2.x API but MUST be verified: the implementer compiles the module against the real dependency (`.\gradlew.bat :axiom-packet:compileJava`) and corrects any API mismatch (wrong class name, renamed method, changed signature) until it compiles. The *structure* of `PacketPipeline` is fixed; only PacketEvents API names may be adjusted. Do not change the testable core to accommodate the glue.

---

## File Structure

```
axiom-libraryac/
├── settings.gradle                        MODIFY — add axiom-packet module
├── axiom-packet/
│   ├── build.gradle                       CREATE — api+math deps, PacketEvents compileOnly
│   └── src/
│       ├── main/java/com/github/axiom/ac/packet/
│       │   ├── MovementUpdate.java         Decoded movement-packet value type
│       │   ├── PlayerDataImpl.java         Concrete PlayerData (mutable, thread-confined)
│       │   ├── PlayerRegistry.java         UUID -> PlayerDataImpl registry
│       │   ├── TransactionSink.java        SPI for sending a transaction packet
│       │   ├── TransactionManager.java     Transaction bookkeeping (latency comp.)
│       │   └── PacketPipeline.java         PacketEvents listener glue
│       └── test/java/com/github/axiom/ac/packet/
│           └── (test classes per the tasks below)
```

**Windows note:** commands use `.\gradlew.bat` (PowerShell).

---

## Task 1: Add the axiom-packet module

**Files:**
- Modify: `settings.gradle`
- Create: `axiom-packet/build.gradle`

- [ ] **Step 1: Replace `settings.gradle`**

```groovy
rootProject.name = 'axiom-libraryac'

include 'axiom-math'
include 'axiom-api'
include 'axiom-packet'
```

- [ ] **Step 2: Create `axiom-packet/build.gradle`**

```groovy
// axiom-packet: PacketEvents-backed packet pipeline. The testable
// core (MovementUpdate, PlayerDataImpl, PlayerRegistry,
// TransactionManager) is PacketEvents-free; only PacketPipeline uses
// PacketEvents, which the server provides at runtime (compileOnly).
repositories {
    maven { url = 'https://repo.codemc.io/repository/maven-releases/' }
}

dependencies {
    implementation project(':axiom-api')
    implementation project(':axiom-math')
    compileOnly 'com.github.retrooper:packetevents-spigot:2.12.1'
}
```

- [ ] **Step 3: Verify the module is recognised**

Run: `.\gradlew.bat projects`
Expected: output lists `Project ':axiom-packet'` alongside the others.

- [ ] **Step 4: Commit**

```bash
git add settings.gradle axiom-packet/build.gradle
git commit -m "build: add axiom-packet module with PacketEvents dependency"
```

---

## Task 2: MovementUpdate — decoded movement-packet value type

**Files:**
- Create: `axiom-packet/src/main/java/com/github/axiom/ac/packet/MovementUpdate.java`
- Test: `axiom-packet/src/test/java/com/github/axiom/ac/packet/MovementUpdateTest.java`

A Minecraft movement packet may carry a position, a rotation, both, or neither (a ground-state-only packet). `MovementUpdate` captures one decoded packet. When `hasPosition` is false, `position` is `null` — consumers must check `hasPosition` first. Static factories cover the four packet shapes.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import org.junit.jupiter.api.Test;

class MovementUpdateTest {

    @Test
    void fullUpdateCarriesPositionAndRotation() {
        MovementUpdate u = MovementUpdate.full(new Vec3(1, 64, 2), 90.0f, -10.0f, true);
        assertTrue(u.hasPosition());
        assertEquals(new Vec3(1, 64, 2), u.position());
        assertTrue(u.hasRotation());
        assertEquals(90.0f, u.yaw());
        assertEquals(-10.0f, u.pitch());
        assertTrue(u.onGround());
    }

    @Test
    void positionOnlyHasNoRotation() {
        MovementUpdate u = MovementUpdate.positionOnly(new Vec3(1, 64, 2), false);
        assertTrue(u.hasPosition());
        assertFalse(u.hasRotation());
        assertFalse(u.onGround());
    }

    @Test
    void rotationOnlyHasNoPosition() {
        MovementUpdate u = MovementUpdate.rotationOnly(45.0f, 5.0f, true);
        assertFalse(u.hasPosition());
        assertNull(u.position());
        assertTrue(u.hasRotation());
        assertEquals(45.0f, u.yaw());
    }

    @Test
    void groundOnlyHasNeitherPositionNorRotation() {
        MovementUpdate u = MovementUpdate.groundOnly(true);
        assertFalse(u.hasPosition());
        assertFalse(u.hasRotation());
        assertTrue(u.onGround());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-packet:test --tests "com.github.axiom.ac.packet.MovementUpdateTest"`
Expected: FAIL — compilation error, `MovementUpdate` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.packet;

import com.github.axiom.ac.math.Vec3;

/**
 * One decoded client movement packet. A Minecraft movement packet
 * may carry a position, a rotation, both, or only a ground flag.
 *
 * <p>When {@code hasPosition} is {@code false}, {@code position} is
 * {@code null}; consumers must check {@code hasPosition} first.
 *
 * @param hasPosition true when the packet carried a position
 * @param position    new position, or {@code null} when absent
 * @param hasRotation true when the packet carried a look angle
 * @param yaw         new horizontal look angle, in degrees
 * @param pitch       new vertical look angle, in degrees
 * @param onGround    client-reported ground state
 */
public record MovementUpdate(boolean hasPosition, Vec3 position,
                             boolean hasRotation, float yaw, float pitch,
                             boolean onGround) {

    /** A packet that carried both a position and a rotation. */
    public static MovementUpdate full(Vec3 position, float yaw, float pitch,
                                      boolean onGround) {
        return new MovementUpdate(true, position, true, yaw, pitch, onGround);
    }

    /** A packet that carried only a position. */
    public static MovementUpdate positionOnly(Vec3 position, boolean onGround) {
        return new MovementUpdate(true, position, false, 0.0f, 0.0f, onGround);
    }

    /** A packet that carried only a rotation. */
    public static MovementUpdate rotationOnly(float yaw, float pitch,
                                              boolean onGround) {
        return new MovementUpdate(false, null, true, yaw, pitch, onGround);
    }

    /** A packet that carried only a ground flag. */
    public static MovementUpdate groundOnly(boolean onGround) {
        return new MovementUpdate(false, null, false, 0.0f, 0.0f, onGround);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-packet:test --tests "com.github.axiom.ac.packet.MovementUpdateTest"`
Expected: PASS — 4 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-packet/src/main/java/com/github/axiom/ac/packet/MovementUpdate.java axiom-packet/src/test/java/com/github/axiom/ac/packet/MovementUpdateTest.java
git commit -m "feat: add MovementUpdate value type to axiom-packet"
```

---

## Task 3: PlayerDataImpl — concrete per-player state

**Files:**
- Create: `axiom-packet/src/main/java/com/github/axiom/ac/packet/PlayerDataImpl.java`
- Test: `axiom-packet/src/test/java/com/github/axiom/ac/packet/PlayerDataImplTest.java`

`PlayerDataImpl` is the concrete `PlayerData` (from `axiom-api`). It is mutable and thread-confined — updated only from the player's netty thread. `applyMovement` advances state: a position update shifts current → previous, recomputes velocity as the delta, and appends to a bounded position history (`RollingBuffer` from `axiom-math`).

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerDataImplTest {

    private static final double EPS = 1e-9;

    @Test
    void rejectsNullUuid() {
        assertThrows(NullPointerException.class, () -> new PlayerDataImpl(null));
    }

    @Test
    void exposesUuid() {
        UUID id = UUID.randomUUID();
        assertEquals(id, new PlayerDataImpl(id).uuid());
    }

    @Test
    void positionUpdateRecomputesVelocity() {
        PlayerDataImpl data = new PlayerDataImpl(UUID.randomUUID());
        data.applyMovement(MovementUpdate.positionOnly(new Vec3(0, 64, 0), true));
        data.applyMovement(MovementUpdate.positionOnly(new Vec3(0.5, 64, 0.25), true));

        assertEquals(new Vec3(0.5, 64, 0.25), data.position());
        assertEquals(new Vec3(0, 64, 0), data.previousPosition());
        assertEquals(0.5, data.velocity().x(), EPS);
        assertEquals(0.0, data.velocity().y(), EPS);
        assertEquals(0.25, data.velocity().z(), EPS);
    }

    @Test
    void rotationUpdateDoesNotTouchPosition() {
        PlayerDataImpl data = new PlayerDataImpl(UUID.randomUUID());
        data.applyMovement(MovementUpdate.positionOnly(new Vec3(1, 64, 1), true));
        data.applyMovement(MovementUpdate.rotationOnly(90.0f, -20.0f, true));

        assertEquals(new Vec3(1, 64, 1), data.position());
        assertEquals(90.0f, data.yaw());
        assertEquals(-20.0f, data.pitch());
    }

    @Test
    void groundFlagAlwaysApplied() {
        PlayerDataImpl data = new PlayerDataImpl(UUID.randomUUID());
        data.applyMovement(MovementUpdate.groundOnly(true));
        assertTrue(data.onGround());
        data.applyMovement(MovementUpdate.groundOnly(false));
        assertEquals(false, data.onGround());
    }

    @Test
    void positionHistoryGrowsWithPositionUpdates() {
        PlayerDataImpl data = new PlayerDataImpl(UUID.randomUUID());
        data.applyMovement(MovementUpdate.positionOnly(new Vec3(0, 64, 0), true));
        data.applyMovement(MovementUpdate.positionOnly(new Vec3(1, 64, 0), true));
        data.applyMovement(MovementUpdate.rotationOnly(10.0f, 0.0f, true));

        assertEquals(2, data.positionHistory().size());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-packet:test --tests "com.github.axiom.ac.packet.PlayerDataImplTest"`
Expected: FAIL — compilation error, `PlayerDataImpl` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.packet;

import com.github.axiom.ac.api.PlayerData;
import com.github.axiom.ac.math.RollingBuffer;
import com.github.axiom.ac.math.Vec3;
import java.util.Objects;
import java.util.UUID;

/**
 * Concrete {@link PlayerData}. Mutable and thread-confined: it is
 * updated only from the owning player's netty thread, so it carries
 * no synchronisation.
 */
public final class PlayerDataImpl implements PlayerData {

    private static final Vec3 ORIGIN = new Vec3(0, 0, 0);
    private static final int HISTORY_CAPACITY = 20;

    private final UUID uuid;
    private final RollingBuffer<Vec3> positionHistory = new RollingBuffer<>(HISTORY_CAPACITY);

    private Vec3 position = ORIGIN;
    private Vec3 previousPosition = ORIGIN;
    private Vec3 velocity = ORIGIN;
    private float yaw;
    private float pitch;
    private boolean onGround;

    public PlayerDataImpl(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
    }

    /**
     * Advances state from one decoded movement packet. A position
     * update shifts current to previous, recomputes velocity as the
     * delta, and appends to the position history. A rotation update
     * sets the look angle. The ground flag is always applied.
     */
    public void applyMovement(MovementUpdate update) {
        if (update.hasPosition()) {
            previousPosition = position;
            position = update.position();
            velocity = position.subtract(previousPosition);
            positionHistory.add(position);
        }
        if (update.hasRotation()) {
            yaw = update.yaw();
            pitch = update.pitch();
        }
        onGround = update.onGround();
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

    @Override
    public Vec3 position() {
        return position;
    }

    @Override
    public Vec3 velocity() {
        return velocity;
    }

    @Override
    public float yaw() {
        return yaw;
    }

    @Override
    public float pitch() {
        return pitch;
    }

    @Override
    public boolean onGround() {
        return onGround;
    }

    /** Position recorded before the most recent position update. */
    public Vec3 previousPosition() {
        return previousPosition;
    }

    /** Bounded history of recent positions, oldest first. */
    public RollingBuffer<Vec3> positionHistory() {
        return positionHistory;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-packet:test --tests "com.github.axiom.ac.packet.PlayerDataImplTest"`
Expected: PASS — 6 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-packet/src/main/java/com/github/axiom/ac/packet/PlayerDataImpl.java axiom-packet/src/test/java/com/github/axiom/ac/packet/PlayerDataImplTest.java
git commit -m "feat: add PlayerDataImpl concrete player state to axiom-packet"
```

---

## Task 4: PlayerRegistry — tracked-player registry

**Files:**
- Create: `axiom-packet/src/main/java/com/github/axiom/ac/packet/PlayerRegistry.java`
- Test: `axiom-packet/src/test/java/com/github/axiom/ac/packet/PlayerRegistryTest.java`

Maps player UUIDs to their `PlayerDataImpl`. Backed by a `ConcurrentHashMap` because registration (player join) and packet processing happen on different threads.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerRegistryTest {

    @Test
    void registerCreatesAndStoresPlayerData() {
        PlayerRegistry registry = new PlayerRegistry();
        UUID id = UUID.randomUUID();

        PlayerDataImpl data = registry.register(id);

        assertEquals(id, data.uuid());
        assertSame(data, registry.get(id).orElseThrow());
    }

    @Test
    void unknownPlayerReturnsEmpty() {
        assertTrue(new PlayerRegistry().get(UUID.randomUUID()).isEmpty());
    }

    @Test
    void unregisterRemovesPlayerData() {
        PlayerRegistry registry = new PlayerRegistry();
        UUID id = UUID.randomUUID();
        registry.register(id);

        registry.unregister(id);

        assertTrue(registry.get(id).isEmpty());
    }

    @Test
    void sizeReflectsRegisteredPlayers() {
        PlayerRegistry registry = new PlayerRegistry();
        assertEquals(0, registry.size());
        registry.register(UUID.randomUUID());
        registry.register(UUID.randomUUID());
        assertEquals(2, registry.size());
    }

    @Test
    void allReturnsEveryRegisteredPlayer() {
        PlayerRegistry registry = new PlayerRegistry();
        registry.register(UUID.randomUUID());
        registry.register(UUID.randomUUID());
        assertEquals(2, registry.all().size());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-packet:test --tests "com.github.axiom.ac.packet.PlayerRegistryTest"`
Expected: FAIL — compilation error, `PlayerRegistry` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.packet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of currently tracked players. Registration happens on the
 * server thread when a player joins; lookups happen on netty threads
 * during packet processing — hence the concurrent map.
 */
public final class PlayerRegistry {

    private final Map<UUID, PlayerDataImpl> players = new ConcurrentHashMap<>();

    /**
     * Creates and stores a fresh {@link PlayerDataImpl} for
     * {@code uuid}, returning it. An existing entry is replaced.
     */
    public PlayerDataImpl register(UUID uuid) {
        PlayerDataImpl data = new PlayerDataImpl(uuid);
        players.put(uuid, data);
        return data;
    }

    /** Stops tracking {@code uuid}. */
    public void unregister(UUID uuid) {
        players.remove(uuid);
    }

    /** The tracked data for {@code uuid}, if any. */
    public Optional<PlayerDataImpl> get(UUID uuid) {
        return Optional.ofNullable(players.get(uuid));
    }

    /** A snapshot of every tracked player. */
    public Collection<PlayerDataImpl> all() {
        return List.copyOf(players.values());
    }

    /** Number of currently tracked players. */
    public int size() {
        return players.size();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-packet:test --tests "com.github.axiom.ac.packet.PlayerRegistryTest"`
Expected: PASS — 5 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-packet/src/main/java/com/github/axiom/ac/packet/PlayerRegistry.java axiom-packet/src/test/java/com/github/axiom/ac/packet/PlayerRegistryTest.java
git commit -m "feat: add PlayerRegistry to axiom-packet"
```

---

## Task 5: TransactionManager and TransactionSink — latency compensation

**Files:**
- Create: `axiom-packet/src/main/java/com/github/axiom/ac/packet/TransactionSink.java`
- Create: `axiom-packet/src/main/java/com/github/axiom/ac/packet/TransactionManager.java`
- Test: `axiom-packet/src/test/java/com/github/axiom/ac/packet/TransactionManagerTest.java`

A transaction is a ping/pong: the server sends a transaction packet, the client echoes it back. The round trip bounds when the client received everything sent before it. `TransactionManager` owns the bookkeeping — id generation, the pending map, round-trip computation — and is PacketEvents-free: the actual packet send is delegated to a `TransactionSink`, so the manager is fully unit-testable with a fake sink.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class TransactionManagerTest {

    /** Fake sink recording every transaction id it was asked to send. */
    private static final class RecordingSink implements TransactionSink {
        final List<Integer> sent = new ArrayList<>();

        @Override
        public void send(int transactionId) {
            sent.add(transactionId);
        }
    }

    @Test
    void rejectsNullSink() {
        assertThrows(NullPointerException.class, () -> new TransactionManager(null));
    }

    @Test
    void sendTransactionForwardsToSinkAndReturnsId() {
        RecordingSink sink = new RecordingSink();
        TransactionManager manager = new TransactionManager(sink);

        int id = manager.sendTransaction(1_000L);

        assertEquals(List.of(id), sink.sent);
        assertTrue(manager.isPending(id));
        assertEquals(1, manager.pendingCount());
    }

    @Test
    void transactionIdsAreUnique() {
        TransactionManager manager = new TransactionManager(new RecordingSink());
        int first = manager.sendTransaction(0L);
        int second = manager.sendTransaction(0L);
        assertFalse(first == second);
    }

    @Test
    void confirmReturnsRoundTripAndClearsPending() {
        TransactionManager manager = new TransactionManager(new RecordingSink());
        int id = manager.sendTransaction(1_000L);

        OptionalLong rtt = manager.confirm(id, 1_050L);

        assertEquals(OptionalLong.of(50L), rtt);
        assertFalse(manager.isPending(id));
        assertEquals(0, manager.pendingCount());
    }

    @Test
    void confirmOfUnknownIdReturnsEmpty() {
        TransactionManager manager = new TransactionManager(new RecordingSink());
        assertEquals(OptionalLong.empty(), manager.confirm(999, 1_000L));
    }

    @Test
    void confirmTwiceReturnsEmptyTheSecondTime() {
        TransactionManager manager = new TransactionManager(new RecordingSink());
        int id = manager.sendTransaction(1_000L);
        manager.confirm(id, 1_010L);
        assertEquals(OptionalLong.empty(), manager.confirm(id, 1_020L));
    }

    @Test
    void lastRoundTripTracksMostRecentConfirm() {
        TransactionManager manager = new TransactionManager(new RecordingSink());
        assertEquals(OptionalLong.empty(), manager.lastRoundTrip());

        int id = manager.sendTransaction(1_000L);
        manager.confirm(id, 1_030L);

        assertEquals(OptionalLong.of(30L), manager.lastRoundTrip());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-packet:test --tests "com.github.axiom.ac.packet.TransactionManagerTest"`
Expected: FAIL — compilation error, the types do not exist.

- [ ] **Step 3: Write the implementations**

Create `TransactionSink.java`:

```java
package com.github.axiom.ac.packet;

/**
 * Sends a transaction (ping) packet carrying the given id to the
 * client. The PacketEvents-backed implementation lives in the
 * packet pipeline; tests supply a fake.
 */
@FunctionalInterface
public interface TransactionSink {

    /** Sends a transaction packet carrying {@code transactionId}. */
    void send(int transactionId);
}
```

Create `TransactionManager.java`:

```java
package com.github.axiom.ac.packet;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Tracks outstanding transactions for latency compensation. Sending
 * a transaction records the send time; confirming it yields the
 * round-trip time, which bounds when the client had received all
 * prior server state.
 *
 * <p>Not thread-safe — one instance per player, used only on that
 * player's netty thread.
 */
public final class TransactionManager {

    private final TransactionSink sink;
    private final Map<Integer, Long> pending = new LinkedHashMap<>();

    private int nextId = 1;
    private long lastRoundTripMillis = -1L;

    public TransactionManager(TransactionSink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * Issues a transaction: allocates a unique id, records
     * {@code nowMillis} as its send time, asks the sink to send it,
     * and returns the id.
     */
    public int sendTransaction(long nowMillis) {
        int id = nextId++;
        pending.put(id, nowMillis);
        sink.send(id);
        return id;
    }

    /**
     * Confirms the transaction {@code id} as echoed back at
     * {@code nowMillis}. Returns the round-trip time in milliseconds,
     * or empty when the id is unknown (never sent, or already
     * confirmed).
     */
    public OptionalLong confirm(int id, long nowMillis) {
        Long sentAt = pending.remove(id);
        if (sentAt == null) {
            return OptionalLong.empty();
        }
        long roundTrip = nowMillis - sentAt;
        lastRoundTripMillis = roundTrip;
        return OptionalLong.of(roundTrip);
    }

    /** True while transaction {@code id} is sent but not yet confirmed. */
    public boolean isPending(int id) {
        return pending.containsKey(id);
    }

    /** Number of sent-but-unconfirmed transactions. */
    public int pendingCount() {
        return pending.size();
    }

    /** Round-trip time of the most recent confirmation, if any. */
    public OptionalLong lastRoundTrip() {
        return lastRoundTripMillis < 0L
                ? OptionalLong.empty()
                : OptionalLong.of(lastRoundTripMillis);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-packet:test --tests "com.github.axiom.ac.packet.TransactionManagerTest"`
Expected: PASS — 7 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-packet/src/main/java/com/github/axiom/ac/packet/TransactionSink.java axiom-packet/src/main/java/com/github/axiom/ac/packet/TransactionManager.java axiom-packet/src/test/java/com/github/axiom/ac/packet/TransactionManagerTest.java
git commit -m "feat: add TransactionManager latency compensation to axiom-packet"
```

---

## Task 6: PacketPipeline — PacketEvents listener glue

**Files:**
- Create: `axiom-packet/src/main/java/com/github/axiom/ac/packet/PacketPipeline.java`

**This task is best-effort glue — see the "IMPORTANT" section at the top of this plan.** The structure below is fixed; PacketEvents 2.12.1 API names must be verified by compiling and corrected if wrong. There is no unit test — the task is verified by `:axiom-packet:compileJava` succeeding.

`PacketPipeline` is a PacketEvents `PacketListenerAbstract`. On each inbound client packet it checks whether the packet is a movement (flying) packet; if so it looks up the player's `PlayerDataImpl` in the `PlayerRegistry`, decodes the packet into a `MovementUpdate`, and applies it.

- [ ] **Step 1: Write the implementation (best-effort)**

```java
package com.github.axiom.ac.packet;

import com.github.axiom.ac.math.Vec3;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import java.util.UUID;

/**
 * PacketEvents listener that feeds decoded client movement packets
 * into the {@link PlayerRegistry}. This is the only PacketEvents-aware
 * type in the module; all decoding logic it depends on
 * ({@link MovementUpdate}, {@link PlayerDataImpl}) is PacketEvents-free.
 */
public final class PacketPipeline extends PacketListenerAbstract {

    private final PlayerRegistry registry;

    public PacketPipeline(PlayerRegistry registry) {
        super(PacketListenerPriority.NORMAL);
        this.registry = registry;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isMovementPacket(event.getPacketType())) {
            return;
        }
        UUID uuid = event.getUser().getUUID();
        if (uuid == null) {
            return;
        }
        PlayerDataImpl data = registry.get(uuid).orElse(null);
        if (data == null) {
            return;
        }
        data.applyMovement(decode(new WrapperPlayClientPlayerFlying(event)));
    }

    /** True when {@code type} is one of the client movement packets. */
    private static boolean isMovementPacket(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || type == PacketType.Play.Client.PLAYER_ROTATION
                || type == PacketType.Play.Client.PLAYER_FLYING;
    }

    /** Decodes a flying-packet wrapper into a {@link MovementUpdate}. */
    static MovementUpdate decode(WrapperPlayClientPlayerFlying wrapper) {
        boolean hasPosition = wrapper.hasPositionChanged();
        boolean hasRotation = wrapper.hasRotationChanged();
        Location location = wrapper.getLocation();
        Vec3 position = hasPosition
                ? new Vec3(location.getX(), location.getY(), location.getZ())
                : null;
        return new MovementUpdate(hasPosition, position, hasRotation,
                location.getYaw(), location.getPitch(), wrapper.isOnGround());
    }
}
```

- [ ] **Step 2: Compile the module and fix any PacketEvents API mismatch**

Run: `.\gradlew.bat :axiom-packet:compileJava`

If compilation fails because a PacketEvents class or method name is wrong, consult the PacketEvents 2.12.1 API (JavaDocs at https://javadocs.packetevents.com or the `packetevents-spigot:2.12.1` jar) and correct the names. Common things to verify:
- `PacketListenerAbstract` constructor and `PacketListenerPriority`.
- `PacketReceiveEvent.getPacketType()` return type and `getUser().getUUID()`.
- `PacketType.Play.Client` movement constant names.
- `WrapperPlayClientPlayerFlying`: `hasPositionChanged()`, `hasRotationChanged()`, `getLocation()`, `isOnGround()`.
- `Location`: `getX()/getY()/getZ()/getYaw()/getPitch()` (or `getPosition()` returning a vector).

Repeat until `BUILD SUCCESSFUL`. Do not alter `MovementUpdate`, `PlayerDataImpl`, or `PlayerRegistry`.

- [ ] **Step 3: Run the whole module test suite to confirm nothing broke**

Run: `.\gradlew.bat :axiom-packet:test`
Expected: PASS — the 22 core tests still green.

- [ ] **Step 4: Commit**

```bash
git add axiom-packet/src/main/java/com/github/axiom/ac/packet/PacketPipeline.java
git commit -m "feat: add PacketPipeline PacketEvents listener to axiom-packet"
```

If PacketEvents API corrections were needed, mention them in the commit body.

---

## Task 7: Full module verification

**Files:** none — verification only.

- [ ] **Step 1: Run the module test suite**

Run: `.\gradlew.bat :axiom-packet:test`
Expected: PASS — `BUILD SUCCESSFUL`, 4 test classes green (22 tests total).

- [ ] **Step 2: Build the whole project**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL` — `axiom-math`, `axiom-api`, `axiom-packet` all build and test green.

- [ ] **Step 3: Commit if anything changed**

If steps 1-2 produced no source changes, skip. Otherwise commit with message `build: verify axiom-packet module builds and tests pass`.

---

## Plan Complete

`axiom-packet` provides the packet pipeline: `MovementUpdate` (decoded packets), `PlayerDataImpl` (concrete mutable player state), `PlayerRegistry` (tracked players), `TransactionManager`/`TransactionSink` (latency compensation), and `PacketPipeline` (PacketEvents glue).

**Next:** Plan 4 — `axiom-world`: server-side world block cache, `CollisionEngine`, and the collision-aware `PhysicsSimulator`. Requires its own plan.
