# Axiom AC — API Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `axiom-api`, the public-contract module of the Axiom AC toolkit: value types, SPIs, the GrimAPI-style event bus, and the read-only player data interface.

**Architecture:** A Gradle module depending only on `axiom-math`. It holds the types consumers compile against. Interfaces (`Check`, `PlayerData`, `StorageProvider`) carry no logic and are verified by test doubles. The event bus (`EventBus`, `EventChannel`, `Subscription`) and value types (`Violation`, `FlagEvent`, `CheckFaultEvent`, lifecycle events) are concrete and unit-tested.

**Tech Stack:** Java 21, Gradle (multi-module), JUnit 6 (Jupiter).

This plan is **Plan 2 of 5** for the Axiom AC toolkit. It implements `axiom-api` from `docs/superpowers/specs/2026-05-18-axiom-ac-design.md` (section 4.1). Plan 1 (`axiom-math`) is already merged.

**Spec refinement:** the design spec placed an `EventBusImpl` in `axiom-core`. This plan instead makes the event bus concrete inside `axiom-api`. Rationale: the bus is the integration surface — consumers should depend only on `axiom-api` to subscribe to events, and the bus carries real, testable logic. `axiom-core` will *use* this bus, not redefine it.

---

## File Structure

```
axiom-libraryac/
├── settings.gradle                       MODIFY — add axiom-api module
├── axiom-api/
│   ├── build.gradle                      CREATE — depends on axiom-math
│   └── src/
│       ├── main/java/com/github/axiom/ac/api/
│       │   ├── Violation.java             Detection result value type
│       │   ├── Cancellable.java           Mix-in interface for cancellable events
│       │   ├── Subscription.java          Handle returned by a subscription
│       │   ├── EventChannel.java          Typed publish/subscribe channel
│       │   ├── EventBus.java              Registry of channels keyed by event type
│       │   ├── PlayerData.java            Read-only per-player state interface
│       │   ├── Check.java                 Detection check SPI
│       │   ├── StorageProvider.java       Persistence SPI
│       │   └── event/
│       │       ├── FlagEvent.java         Fired when a check flags a player
│       │       ├── CheckFaultEvent.java   Fired when a check is auto-disabled
│       │       ├── PlayerJoinEvent.java   Fired when Axiom starts tracking a player
│       │       └── PlayerQuitEvent.java   Fired when tracking ends
│       └── test/java/com/github/axiom/ac/api/
│           └── (test classes per the tasks below)
```

Package layout: core contracts in `com.github.axiom.ac.api`; concrete event types in `com.github.axiom.ac.api.event`.

**Windows note:** commands use `.\gradlew.bat` (PowerShell). On Unix substitute `./gradlew`.

---

## Task 1: Add the axiom-api module

**Files:**
- Modify: `settings.gradle`
- Create: `axiom-api/build.gradle`

- [ ] **Step 1: Replace `settings.gradle`**

```groovy
rootProject.name = 'axiom-libraryac'

include 'axiom-math'
include 'axiom-api'
```

- [ ] **Step 2: Create `axiom-api/build.gradle`**

```groovy
// axiom-api: public contracts for the Axiom AC toolkit. Depends only
// on axiom-math, whose geometry types appear in the public API.
// Build configuration is inherited from the root `subprojects` block.
dependencies {
    implementation project(':axiom-math')
}
```

- [ ] **Step 3: Verify the module is recognised**

Run: `.\gradlew.bat projects`
Expected: output lists both `Project ':axiom-math'` and `Project ':axiom-api'`.

- [ ] **Step 4: Commit**

```bash
git add settings.gradle axiom-api/build.gradle
git commit -m "build: add axiom-api module depending on axiom-math"
```

---

## Task 2: Violation — detection result value type

**Files:**
- Create: `axiom-api/src/main/java/com/github/axiom/ac/api/Violation.java`
- Test: `axiom-api/src/test/java/com/github/axiom/ac/api/ViolationTest.java`

A `Violation` is what a check emits when it detects something. `value` is a check-defined magnitude (offset, score). `confidence` is normalised to `[0, 1]`.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ViolationTest {

    @Test
    void exposesItsFields() {
        Violation v = new Violation("speed", "moved too fast", 1.5, 0.8);
        assertEquals("speed", v.checkId());
        assertEquals("moved too fast", v.description());
        assertEquals(1.5, v.value());
        assertEquals(0.8, v.confidence());
    }

    @Test
    void rejectsNullCheckId() {
        assertThrows(NullPointerException.class,
                () -> new Violation(null, "desc", 1.0, 0.5));
    }

    @Test
    void rejectsNullDescription() {
        assertThrows(NullPointerException.class,
                () -> new Violation("speed", null, 1.0, 0.5));
    }

    @Test
    void rejectsConfidenceBelowZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new Violation("speed", "desc", 1.0, -0.1));
    }

    @Test
    void rejectsConfidenceAboveOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new Violation("speed", "desc", 1.0, 1.1));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-api:test --tests "com.github.axiom.ac.api.ViolationTest"`
Expected: FAIL — compilation error, `Violation` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.api;

import java.util.Objects;

/**
 * The result a {@link Check} emits when it detects suspicious
 * behaviour.
 *
 * @param checkId     id of the check that produced this violation
 * @param description human-readable explanation
 * @param value       check-defined magnitude (for example an offset
 *                    or score); interpretation is up to the check
 * @param confidence  detection confidence, normalised to {@code [0, 1]}
 */
public record Violation(String checkId, String description,
                        double value, double confidence) {

    public Violation {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(description, "description");
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be in [0, 1]");
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-api:test --tests "com.github.axiom.ac.api.ViolationTest"`
Expected: PASS — 5 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-api/src/main/java/com/github/axiom/ac/api/Violation.java axiom-api/src/test/java/com/github/axiom/ac/api/ViolationTest.java
git commit -m "feat: add Violation value type to axiom-api"
```

---

## Task 3: Cancellable and Subscription interfaces

**Files:**
- Create: `axiom-api/src/main/java/com/github/axiom/ac/api/Cancellable.java`
- Create: `axiom-api/src/main/java/com/github/axiom/ac/api/Subscription.java`

These are pure interfaces with no logic. They are verified by compilation and exercised indirectly by later tasks (`EventChannel`, `FlagEvent`). No dedicated test.

- [ ] **Step 1: Create `Cancellable.java`**

```java
package com.github.axiom.ac.api;

/**
 * Mix-in for events that a subscriber can cancel. A cancelled event
 * signals that its default consequence (for example a punishment)
 * should not happen.
 */
public interface Cancellable {

    /** True when a subscriber has cancelled this event. */
    boolean isCancelled();

    /** Sets the cancelled state of this event. */
    void setCancelled(boolean cancelled);
}
```

- [ ] **Step 2: Create `Subscription.java`**

```java
package com.github.axiom.ac.api;

/**
 * Handle to a single event subscription. Closing it removes the
 * handler from its {@link EventChannel}.
 */
@FunctionalInterface
public interface Subscription {

    /** Removes the associated handler from its channel. */
    void unsubscribe();
}
```

- [ ] **Step 3: Verify it compiles**

Run: `.\gradlew.bat :axiom-api:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add axiom-api/src/main/java/com/github/axiom/ac/api/Cancellable.java axiom-api/src/main/java/com/github/axiom/ac/api/Subscription.java
git commit -m "feat: add Cancellable and Subscription interfaces to axiom-api"
```

---

## Task 4: EventChannel — typed publish/subscribe channel

**Files:**
- Create: `axiom-api/src/main/java/com/github/axiom/ac/api/EventChannel.java`
- Test: `axiom-api/src/test/java/com/github/axiom/ac/api/EventChannelTest.java`

A channel holds the subscribers for one event type. `subscribe` returns a `Subscription` whose `unsubscribe()` removes the handler. Backed by a `CopyOnWriteArrayList` so publishing is safe while handlers are added or removed.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class EventChannelTest {

    @Test
    void publishDeliversToSubscriber() {
        EventChannel<String> channel = new EventChannel<>();
        AtomicInteger calls = new AtomicInteger();
        channel.subscribe(event -> calls.incrementAndGet());

        channel.publish("hello");

        assertEquals(1, calls.get());
    }

    @Test
    void publishDeliversToEverySubscriber() {
        EventChannel<String> channel = new EventChannel<>();
        AtomicInteger calls = new AtomicInteger();
        channel.subscribe(event -> calls.incrementAndGet());
        channel.subscribe(event -> calls.incrementAndGet());

        channel.publish("hello");

        assertEquals(2, calls.get());
    }

    @Test
    void unsubscribeStopsDelivery() {
        EventChannel<String> channel = new EventChannel<>();
        AtomicInteger calls = new AtomicInteger();
        Subscription subscription = channel.subscribe(event -> calls.incrementAndGet());

        subscription.unsubscribe();
        channel.publish("hello");

        assertEquals(0, calls.get());
    }

    @Test
    void subscriberCountReflectsSubscriptions() {
        EventChannel<String> channel = new EventChannel<>();
        assertEquals(0, channel.subscriberCount());
        Subscription s = channel.subscribe(event -> { });
        assertEquals(1, channel.subscriberCount());
        s.unsubscribe();
        assertEquals(0, channel.subscriberCount());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-api:test --tests "com.github.axiom.ac.api.EventChannelTest"`
Expected: FAIL — compilation error, `EventChannel` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Typed publish/subscribe channel for a single event type. Handlers
 * are invoked synchronously on the publishing thread, in subscription
 * order.
 *
 * <p>Safe to publish while handlers are being added or removed.
 *
 * @param <T> event type carried by this channel
 */
public final class EventChannel<T> {

    private final List<Consumer<T>> handlers = new CopyOnWriteArrayList<>();

    /**
     * Registers {@code handler}. The returned {@link Subscription}
     * removes it when closed.
     */
    public Subscription subscribe(Consumer<T> handler) {
        handlers.add(handler);
        return () -> handlers.remove(handler);
    }

    /** Delivers {@code event} to every current subscriber. */
    public void publish(T event) {
        for (Consumer<T> handler : handlers) {
            handler.accept(event);
        }
    }

    /** Number of currently registered subscribers. */
    public int subscriberCount() {
        return handlers.size();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-api:test --tests "com.github.axiom.ac.api.EventChannelTest"`
Expected: PASS — 4 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-api/src/main/java/com/github/axiom/ac/api/EventChannel.java axiom-api/src/test/java/com/github/axiom/ac/api/EventChannelTest.java
git commit -m "feat: add EventChannel pub/sub channel to axiom-api"
```

---

## Task 5: EventBus — channel registry keyed by event type

**Files:**
- Create: `axiom-api/src/main/java/com/github/axiom/ac/api/EventBus.java`
- Test: `axiom-api/src/test/java/com/github/axiom/ac/api/EventBusTest.java`

The bus owns one `EventChannel` per event class. `channel(Class)` lazily creates and returns the channel for a type. `publish(event)` routes to the channel of the event's runtime class.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class EventBusTest {

    @Test
    void channelForSameTypeIsReused() {
        EventBus bus = new EventBus();
        EventChannel<String> first = bus.channel(String.class);
        EventChannel<String> second = bus.channel(String.class);
        assertSame(first, second);
    }

    @Test
    void publishRoutesToChannelOfEventType() {
        EventBus bus = new EventBus();
        AtomicReference<String> received = new AtomicReference<>();
        bus.channel(String.class).subscribe(received::set);

        bus.publish("hello");

        assertEquals("hello", received.get());
    }

    @Test
    void publishDoesNotReachOtherTypes() {
        EventBus bus = new EventBus();
        AtomicReference<Integer> received = new AtomicReference<>();
        bus.channel(Integer.class).subscribe(received::set);

        bus.publish("hello");

        assertEquals(null, received.get());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-api:test --tests "com.github.axiom.ac.api.EventBusTest"`
Expected: FAIL — compilation error, `EventBus` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link EventChannel}s, one per event type. This is the
 * GrimAPI-style integration surface: consumers obtain a channel with
 * {@link #channel(Class)} and subscribe to it.
 */
public final class EventBus {

    private final Map<Class<?>, EventChannel<?>> channels = new ConcurrentHashMap<>();

    /**
     * Returns the channel for {@code type}, creating it on first
     * request. The same channel instance is returned for every later
     * call with the same type.
     */
    @SuppressWarnings("unchecked")
    public <T> EventChannel<T> channel(Class<T> type) {
        return (EventChannel<T>) channels.computeIfAbsent(type, key -> new EventChannel<>());
    }

    /**
     * Publishes {@code event} to the channel of its runtime class.
     * If no channel exists for that class yet, the event is dropped.
     */
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        EventChannel<T> channel = (EventChannel<T>) channels.get(event.getClass());
        if (channel != null) {
            channel.publish(event);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-api:test --tests "com.github.axiom.ac.api.EventBusTest"`
Expected: PASS — 3 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-api/src/main/java/com/github/axiom/ac/api/EventBus.java axiom-api/src/test/java/com/github/axiom/ac/api/EventBusTest.java
git commit -m "feat: add EventBus channel registry to axiom-api"
```

---

## Task 6: FlagEvent — cancellable flag event

**Files:**
- Create: `axiom-api/src/main/java/com/github/axiom/ac/api/event/FlagEvent.java`
- Test: `axiom-api/src/test/java/com/github/axiom/ac/api/event/FlagEventTest.java`

`FlagEvent` is fired when a check flags a player. It is `Cancellable` — a subscriber cancelling it tells the runtime to suppress the default consequence.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.api.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.api.Violation;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FlagEventTest {

    private final UUID player = UUID.randomUUID();
    private final Violation violation = new Violation("speed", "too fast", 1.0, 0.9);

    @Test
    void exposesPlayerAndViolation() {
        FlagEvent event = new FlagEvent(player, violation);
        assertEquals(player, event.playerId());
        assertEquals(violation, event.violation());
    }

    @Test
    void isNotCancelledByDefault() {
        assertFalse(new FlagEvent(player, violation).isCancelled());
    }

    @Test
    void canBeCancelled() {
        FlagEvent event = new FlagEvent(player, violation);
        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new FlagEvent(null, violation));
        assertThrows(NullPointerException.class, () -> new FlagEvent(player, null));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-api:test --tests "com.github.axiom.ac.api.event.FlagEventTest"`
Expected: FAIL — compilation error, `FlagEvent` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.api.event;

import com.github.axiom.ac.api.Cancellable;
import com.github.axiom.ac.api.Violation;
import java.util.Objects;
import java.util.UUID;

/**
 * Fired when a check flags a player with a {@link Violation}.
 * Cancelling this event asks the runtime to suppress the default
 * consequence (for example a configured punishment).
 */
public final class FlagEvent implements Cancellable {

    private final UUID playerId;
    private final Violation violation;
    private boolean cancelled;

    public FlagEvent(UUID playerId, Violation violation) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.violation = Objects.requireNonNull(violation, "violation");
    }

    /** Id of the flagged player. */
    public UUID playerId() {
        return playerId;
    }

    /** The violation that triggered this event. */
    public Violation violation() {
        return violation;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-api:test --tests "com.github.axiom.ac.api.event.FlagEventTest"`
Expected: PASS — 4 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-api/src/main/java/com/github/axiom/ac/api/event/FlagEvent.java axiom-api/src/test/java/com/github/axiom/ac/api/event/FlagEventTest.java
git commit -m "feat: add FlagEvent to axiom-api"
```

---

## Task 7: Lifecycle and fault events

**Files:**
- Create: `axiom-api/src/main/java/com/github/axiom/ac/api/event/PlayerJoinEvent.java`
- Create: `axiom-api/src/main/java/com/github/axiom/ac/api/event/PlayerQuitEvent.java`
- Create: `axiom-api/src/main/java/com/github/axiom/ac/api/event/CheckFaultEvent.java`
- Test: `axiom-api/src/test/java/com/github/axiom/ac/api/event/LifecycleEventsTest.java`

`PlayerJoinEvent` / `PlayerQuitEvent` mark when Axiom starts and stops tracking a player. `CheckFaultEvent` is fired when a check is auto-disabled after repeated exceptions. All three are simple immutable records, observational (not cancellable).

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.api.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class LifecycleEventsTest {

    private final UUID player = UUID.randomUUID();

    @Test
    void playerJoinEventCarriesPlayerId() {
        assertEquals(player, new PlayerJoinEvent(player).playerId());
    }

    @Test
    void playerQuitEventCarriesPlayerId() {
        assertEquals(player, new PlayerQuitEvent(player).playerId());
    }

    @Test
    void checkFaultEventCarriesCheckIdAndReason() {
        CheckFaultEvent event = new CheckFaultEvent("speed", "3 consecutive exceptions");
        assertEquals("speed", event.checkId());
        assertEquals("3 consecutive exceptions", event.reason());
    }

    @Test
    void eventsRejectNullArguments() {
        assertThrows(NullPointerException.class, () -> new PlayerJoinEvent(null));
        assertThrows(NullPointerException.class, () -> new PlayerQuitEvent(null));
        assertThrows(NullPointerException.class, () -> new CheckFaultEvent(null, "r"));
        assertThrows(NullPointerException.class, () -> new CheckFaultEvent("speed", null));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-api:test --tests "com.github.axiom.ac.api.event.LifecycleEventsTest"`
Expected: FAIL — compilation error, the event types do not exist.

- [ ] **Step 3: Write the implementations**

Create `PlayerJoinEvent.java`:

```java
package com.github.axiom.ac.api.event;

import java.util.Objects;
import java.util.UUID;

/**
 * Fired when Axiom starts tracking a player.
 *
 * @param playerId id of the player now being tracked
 */
public record PlayerJoinEvent(UUID playerId) {

    public PlayerJoinEvent {
        Objects.requireNonNull(playerId, "playerId");
    }
}
```

Create `PlayerQuitEvent.java`:

```java
package com.github.axiom.ac.api.event;

import java.util.Objects;
import java.util.UUID;

/**
 * Fired when Axiom stops tracking a player.
 *
 * @param playerId id of the player no longer being tracked
 */
public record PlayerQuitEvent(UUID playerId) {

    public PlayerQuitEvent {
        Objects.requireNonNull(playerId, "playerId");
    }
}
```

Create `CheckFaultEvent.java`:

```java
package com.github.axiom.ac.api.event;

import java.util.Objects;

/**
 * Fired when a check is auto-disabled after repeatedly throwing
 * exceptions during inspection.
 *
 * @param checkId id of the faulted check
 * @param reason  human-readable explanation of the fault
 */
public record CheckFaultEvent(String checkId, String reason) {

    public CheckFaultEvent {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(reason, "reason");
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-api:test --tests "com.github.axiom.ac.api.event.LifecycleEventsTest"`
Expected: PASS — 4 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-api/src/main/java/com/github/axiom/ac/api/event/ axiom-api/src/test/java/com/github/axiom/ac/api/event/LifecycleEventsTest.java
git commit -m "feat: add lifecycle and fault events to axiom-api"
```

---

## Task 8: PlayerData — read-only player state interface

**Files:**
- Create: `axiom-api/src/main/java/com/github/axiom/ac/api/PlayerData.java`
- Test: `axiom-api/src/test/java/com/github/axiom/ac/api/PlayerDataTest.java`

`PlayerData` is the read-only view of a tracked player that checks inspect. It is a pure interface — the concrete implementation lives in `axiom-packet` (a later plan). The test verifies the contract by implementing a minimal test double. Geometry types come from `axiom-math`.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerDataTest {

    /** Minimal test double proving the PlayerData contract is implementable. */
    private record FakePlayerData(UUID uuid, Vec3 position, Vec3 velocity,
                                  float yaw, float pitch, boolean onGround)
            implements PlayerData {
    }

    @Test
    void implementationExposesState() {
        UUID id = UUID.randomUUID();
        PlayerData data = new FakePlayerData(
                id, new Vec3(1, 64, 2), new Vec3(0.1, 0, 0.1), 90.0f, -10.0f, true);

        assertEquals(id, data.uuid());
        assertEquals(new Vec3(1, 64, 2), data.position());
        assertEquals(new Vec3(0.1, 0, 0.1), data.velocity());
        assertEquals(90.0f, data.yaw());
        assertEquals(-10.0f, data.pitch());
        assertTrue(data.onGround());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-api:test --tests "com.github.axiom.ac.api.PlayerDataTest"`
Expected: FAIL — compilation error, `PlayerData` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.api;

import com.github.axiom.ac.math.Vec3;
import java.util.UUID;

/**
 * Read-only view of a tracked player, inspected by {@link Check}s.
 * The concrete implementation is provided by the packet pipeline;
 * checks must treat this purely as a data source and must not call
 * the Bukkit API through it.
 */
public interface PlayerData {

    /** Unique id of the player. */
    UUID uuid();

    /** Current position. */
    Vec3 position();

    /** Current velocity (movement delta over the last tick). */
    Vec3 velocity();

    /** Current horizontal look angle, in degrees. */
    float yaw();

    /** Current vertical look angle, in degrees. */
    float pitch();

    /** True when the player is reported as standing on ground. */
    boolean onGround();
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-api:test --tests "com.github.axiom.ac.api.PlayerDataTest"`
Expected: PASS — 1 test.

- [ ] **Step 5: Commit**

```bash
git add axiom-api/src/main/java/com/github/axiom/ac/api/PlayerData.java axiom-api/src/test/java/com/github/axiom/ac/api/PlayerDataTest.java
git commit -m "feat: add PlayerData read-only interface to axiom-api"
```

---

## Task 9: Check — detection check SPI

**Files:**
- Create: `axiom-api/src/main/java/com/github/axiom/ac/api/Check.java`
- Test: `axiom-api/src/test/java/com/github/axiom/ac/api/CheckTest.java`

`Check` is the SPI consumers implement to write detection logic. `inspect` examines a `PlayerData` snapshot and returns an optional `Violation`. The test implements a trivial check to prove the contract works end to end.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckTest {

    private record FakePlayerData(UUID uuid, Vec3 position, Vec3 velocity,
                                  float yaw, float pitch, boolean onGround)
            implements PlayerData {
    }

    /** Flags when horizontal speed exceeds a fixed threshold. */
    private static final class SpeedCheck implements Check {
        @Override
        public String id() {
            return "speed";
        }

        @Override
        public Optional<Violation> inspect(PlayerData data) {
            double horizontal = Math.hypot(data.velocity().x(), data.velocity().z());
            if (horizontal > 1.0) {
                return Optional.of(new Violation(id(), "too fast", horizontal, 1.0));
            }
            return Optional.empty();
        }
    }

    private PlayerData withVelocity(Vec3 velocity) {
        return new FakePlayerData(UUID.randomUUID(), new Vec3(0, 0, 0),
                velocity, 0.0f, 0.0f, true);
    }

    @Test
    void checkExposesItsId() {
        assertEquals("speed", new SpeedCheck().id());
    }

    @Test
    void checkFlagsExcessiveSpeed() {
        Optional<Violation> result = new SpeedCheck().inspect(withVelocity(new Vec3(2, 0, 0)));
        assertTrue(result.isPresent());
        assertEquals("speed", result.get().checkId());
    }

    @Test
    void checkPassesNormalSpeed() {
        Optional<Violation> result = new SpeedCheck().inspect(withVelocity(new Vec3(0.1, 0, 0)));
        assertTrue(result.isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-api:test --tests "com.github.axiom.ac.api.CheckTest"`
Expected: FAIL — compilation error, `Check` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.api;

import java.util.Optional;

/**
 * Detection check SPI. Consumers implement this to add detection
 * logic. A check is stateless — any per-player state belongs in the
 * {@link PlayerData} implementation or in storage. Implementations
 * must be safe to call from the packet-processing thread and must
 * not call the Bukkit API directly.
 */
public interface Check {

    /** Stable, unique id of this check (for example {@code "speed"}). */
    String id();

    /**
     * Inspects a player snapshot. Returns a {@link Violation} when
     * the check detects something, or empty otherwise.
     */
    Optional<Violation> inspect(PlayerData data);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-api:test --tests "com.github.axiom.ac.api.CheckTest"`
Expected: PASS — 3 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-api/src/main/java/com/github/axiom/ac/api/Check.java axiom-api/src/test/java/com/github/axiom/ac/api/CheckTest.java
git commit -m "feat: add Check detection SPI to axiom-api"
```

---

## Task 10: StorageProvider — persistence SPI

**Files:**
- Create: `axiom-api/src/main/java/com/github/axiom/ac/api/StorageProvider.java`
- Test: `axiom-api/src/test/java/com/github/axiom/ac/api/StorageProviderTest.java`

`StorageProvider` is the pluggable persistence SPI. The default in-memory and JSON implementations land in `axiom-core` (a later plan). The test implements a minimal in-memory double to prove the contract.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StorageProviderTest {

    /** Minimal in-memory test double proving the StorageProvider contract. */
    private static final class MemoryStorage implements StorageProvider {
        private final Map<UUID, List<Violation>> byPlayer = new HashMap<>();

        @Override
        public void saveViolation(UUID playerId, Violation violation) {
            byPlayer.computeIfAbsent(playerId, key -> new ArrayList<>()).add(violation);
        }

        @Override
        public List<Violation> loadViolations(UUID playerId) {
            return List.copyOf(byPlayer.getOrDefault(playerId, List.of()));
        }
    }

    @Test
    void savedViolationIsLoadedBack() {
        MemoryStorage storage = new MemoryStorage();
        UUID player = UUID.randomUUID();
        Violation violation = new Violation("speed", "too fast", 1.0, 0.9);

        storage.saveViolation(player, violation);

        assertEquals(List.of(violation), storage.loadViolations(player));
    }

    @Test
    void unknownPlayerHasNoViolations() {
        assertTrue(new MemoryStorage().loadViolations(UUID.randomUUID()).isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-api:test --tests "com.github.axiom.ac.api.StorageProviderTest"`
Expected: FAIL — compilation error, `StorageProvider` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.api;

import java.util.List;
import java.util.UUID;

/**
 * Pluggable persistence SPI. The runtime ships in-memory and JSON
 * implementations; consumers may supply their own (for example
 * SQL-backed) implementation.
 */
public interface StorageProvider {

    /** Persists {@code violation} for the given player. */
    void saveViolation(UUID playerId, Violation violation);

    /**
     * Returns all stored violations for {@code playerId}, or an empty
     * list when none exist.
     */
    List<Violation> loadViolations(UUID playerId);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-api:test --tests "com.github.axiom.ac.api.StorageProviderTest"`
Expected: PASS — 2 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-api/src/main/java/com/github/axiom/ac/api/StorageProvider.java axiom-api/src/test/java/com/github/axiom/ac/api/StorageProviderTest.java
git commit -m "feat: add StorageProvider persistence SPI to axiom-api"
```

---

## Task 11: Full module verification

**Files:** none — verification only.

- [ ] **Step 1: Run the whole module test suite**

Run: `.\gradlew.bat :axiom-api:test`
Expected: PASS — `BUILD SUCCESSFUL`, all 8 test classes green (26 tests total).

- [ ] **Step 2: Build the module**

Run: `.\gradlew.bat :axiom-api:build`
Expected: `BUILD SUCCESSFUL`. `axiom-api/build/libs/axiom-api-1.0-SNAPSHOT.jar` is produced.

- [ ] **Step 3: Build the whole project**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL` — `axiom-math` and `axiom-api` both build and test green.

- [ ] **Step 4: Commit if anything changed**

If steps 1-3 produced no source changes, skip. Otherwise commit with message `build: verify axiom-api module builds and tests pass`.

---

## Plan Complete

`axiom-api` is a complete, fully-tested public-contract module: the `Violation` value type, the `EventBus`/`EventChannel`/`Subscription` event system, the `FlagEvent`/`CheckFaultEvent`/`PlayerJoinEvent`/`PlayerQuitEvent` event types, and the `PlayerData`/`Check`/`StorageProvider` SPIs.

**Next:** Plan 3 — `axiom-packet`: PacketEvents pipeline, the concrete `PlayerData` implementation, and the `TransactionManager` for latency compensation. Requires its own plan.
