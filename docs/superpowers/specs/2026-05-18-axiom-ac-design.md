# Axiom AC — Design Specification

**Date:** 2026-05-18
**Status:** Approved (design phase)
**Project:** `axiom-libraryac` — anticheat toolkit library

## 1. Purpose and Scope

Axiom AC is a Minecraft (Paper) anticheat **toolkit library**. It is not a
finished anticheat: it ships **no detection checks**. Its value is the
framework — mathematical primitives, a packet-processing pipeline with latency
compensation, server-side world state tracking, and a GrimAPI-style event bus —
that developers use to build their own checks.

Two faces:

- **Detection toolkit** — math primitives + packet pipeline + world/physics
  state, consumed to write custom `Check` implementations.
- **Integration API** — event bus exposing flags, transactions, and player
  lifecycle so third-party plugins integrate with a running instance.

### In scope

- Pure mathematical toolkit (statistics, distribution analysis, GCD/periodicity,
  outlier detection, 3D geometry, physics formulas).
- Packet interception pipeline via PacketEvents, multi-version capable.
- Transaction system (ping/pong) for latency compensation.
- Server-side world state cache + collision engine + physics simulator with
  block collision.
- Event bus and read-only player data API.
- Pluggable persistence (`StorageProvider`).
- Hybrid runtime (embeddable or shared-singleton plugin).

### Out of scope (this cycle)

- Built-in detection checks — none, by design.
- `axiom-predict` deterministic tick-by-tick prediction engine — designed here,
  implemented in Phase 2.
- Built-in punishment, ban management, GUI/commands.

## 2. Targets and Stack

- **Platform:** Paper 1.21.x
- **Language/runtime:** Java 21
- **Build:** Gradle multi-module
- **Packet layer:** PacketEvents 2.x (provides multi-version packet abstraction)
- **Test:** JUnit 6 (already in `build.gradle`), MockBukkit for integration

## 3. Module Architecture

```
axiom-libraryac/
├── axiom-api       Public contracts. No implementation. Event bus interfaces,
│                   PlayerData (read-only), StorageProvider SPI, Check SPI,
│                   Violation/Flag types. The artifact consumers compile against.
├── axiom-math      Pure mathematical toolkit. No MC / PacketEvents dependency.
│                   Statistics, distribution, GCD, outliers, 3D geometry,
│                   physics formulas (gravity/friction/jump). ~100% unit-tested.
├── axiom-packet    PacketEvents integration. Packet pipeline, per-player
│                   buffers, transaction system (ping/pong latency
│                   compensation). Multi-version via PacketEvents.
├── axiom-world     Server-side world state. Block cache around players, chunk
│                   diffing, CollisionEngine (AABB vs world), PhysicsSimulator
│                   (axiom-math formulas + collision).
├── axiom-core      Wiring. Hybrid runtime bootstrap, event bus impl, default
│                   StorageProvider (in-memory + JSON), player lifecycle,
│                   Check registry.
├── axiom-plugin    Thin Paper plugin. onEnable: bootstraps core as a shared
│                   singleton, registers PacketEvents.
└── axiom-predict   [PHASE 2] Deterministic tick-by-tick movement prediction
                    engine. Designed here, implemented later.
```

### Dependency graph

- `axiom-api` — depended on by all modules.
- `axiom-math` — standalone, no dependencies.
- `axiom-packet` → `api`, `math`.
- `axiom-world` → `api`, `math`, `packet`.
- `axiom-core` → `api`, `math`, `packet`, `world`.
- `axiom-plugin` → `core`.
- `axiom-predict` → `core`, `world`.

`axiom-math` is deliberately isolated and dependency-free: independently
testable and reusable. `axiom-world` is optional — consumers that do not need
collision-aware physics simulation never load it.

## 4. Components

### 4.1 axiom-api (public surface)

- `Check` (SPI) — `void onPlayerData(PlayerData)`. Logic is stateless; state
  lives in `PlayerData`.
- `CheckContext` — passed to a check: access to the math toolkit,
  `CollisionEngine`, and transaction data. Exposes only toolkit-safe data; no
  direct Bukkit API access.
- `PlayerData` (read API) — current and historical position/velocity, rotation,
  packet timing, on-ground state, sample buffers.
- `Violation` — check id, description, value (offset/score), confidence.
- `EventBus` + `EventChannel<T>` — GrimAPI-style:
  `bus.get(FlagEvent.class).subscribe(...)`.
- Events: `PlayerJoinEvent`, `PlayerQuitEvent`, `FlagEvent` (cancellable),
  `TransactionEvent`, `CheckFaultEvent`, `AxiomLoadEvent`.
- `StorageProvider` (SPI) — save/load violations and arbitrary keyed data.
- `AxiomProvider.get()` — access to the runtime instance.

### 4.2 axiom-math

- `RollingBuffer<T>`, `SlidingWindow` — sliding windows over samples.
- `Stats` — mean, variance, standard deviation, min/max.
- `Distribution` — skewness, kurtosis, Shannon entropy.
- `Gcd` — periodicity analysis (common divisor of intervals).
- `Outliers` — z-score, IQR, adaptive thresholds.
- `Vec3`, `Ray`, `Aabb` — 3D geometry; ray-box intersection, distances, angles.
- `MotionFormulas` — gravity, friction, jump motion, drag (pure formulas, no
  collision).

### 4.3 axiom-packet

- `PacketPipeline` — hooks PacketEvents, decodes, routes packets.
- `PlayerDataImpl` — per-player buffers, thread-confined to the netty channel.
- `TransactionManager` — sends transaction packets, tracks acks, timestamps
  states with a temporal range. Foundation of latency compensation.

### 4.4 axiom-world

- `WorldCache` — blocks cached around the player, updated from block changes.
- `CollisionEngine` — AABB-vs-world queries, block raycasting.
- `PhysicsSimulator` — combines `MotionFormulas` + `CollisionEngine` to predict
  the next-tick position with collision.

### 4.5 axiom-core

- `AxiomRuntime` — hybrid bootstrap, lifecycle, Check registry.
- `EventBusImpl`, `MemoryStorageProvider`, `JsonStorageProvider`.

## 5. Data Flow

Per inbound client packet:

1. PacketEvents receives the packet on the netty event-loop thread.
2. `PacketPipeline` decodes it and updates that player's `PlayerData`
   (thread-confined to the netty channel — no locking).
3. `TransactionManager` sends a transaction packet; on ack it stamps each state
   with a temporal range, providing latency compensation.
4. `WorldCache` is updated from block changes: captured on the main thread,
   published as immutable snapshots to the player worker.
5. On a complete movement packet, `AxiomRuntime` invokes every registered
   `Check` with a `CheckContext` (math toolkit, `CollisionEngine`,
   transaction data).
6. A check produces a `Violation`, emitted as a `FlagEvent` on the event bus.
7. Consumers subscribe to `FlagEvent`, decide punishment; `StorageProvider`
   logs if configured.

## 6. Threading Model

- Per-player processing runs on the player's netty event-loop thread.
  `PlayerData` is thread-confined — no synchronization required.
- World block updates are captured on the main thread and handed to player
  workers as immutable snapshots.
- Event bus dispatch is synchronous on the processing thread. Consumers are
  responsible for bouncing to the main thread for Bukkit API calls.
- The math toolkit is pure, stateless, and thread-safe by construction.

**Contract:** checks must not call the Bukkit API directly (wrong thread).
`CheckContext` exposes only toolkit-safe data. Documented as a hard contract.

## 7. Error Handling

- **Check exception** — isolated in a try/catch in the pipeline. A throwing
  check does not kill processing. Logged; after N consecutive faults the check
  is auto-disabled and a `CheckFaultEvent` is emitted.
- **Packet decode failure** — logged, packet skipped, player flagged
  `bad-packet` via event. Consumer decides.
- **World desync** — `CollisionEngine` queries on uncached blocks return
  `Optional.empty()` / `UNKNOWN` state, never an exception. The check decides
  how to treat uncertainty.
- **Bootstrap** — if PacketEvents is absent or the MC version is unsupported,
  bootstrap fails explicitly with a clear message; the runtime does not start.
- **Double bootstrap** (embedded + plugin) — the hybrid runtime detects an
  existing singleton, reuses the shared instance, and logs a warning.

## 8. Testing Strategy

- `axiom-math` — pure unit tests, ~100% coverage, deterministic. The foundation
  of trust: if the math is correct, checks built on it are reliable.
- `axiom-packet` / `axiom-world` — PacketEvents mocked; replay of recorded
  packet captures (fixtures).
- `axiom-core` — lifecycle, event bus, and storage provider tests.
- `axiom-plugin` — integration tests with MockBukkit.
- **Replay harness** — a recorded packet session is re-run and assertions are
  made against resulting `PlayerData`/violations. Enables physics regression
  testing without a live server.

## 9. Distribution

- `axiom-api` and `axiom-math` published to Maven (jitpack) for consumers to
  compile against.
- `axiom-plugin` distributed as a shaded jar.
- Versioning: semantic; `1.0` targets the toolkit (Approach 2). `axiom-predict`
  lands in a later minor/major.

## 10. Phasing

- **Phase 1 (this spec):** `api`, `math`, `packet`, `world`, `core`, `plugin` —
  the full Approach 2 toolkit.
- **Phase 2:** `axiom-predict` — deterministic tick-by-tick prediction engine
  layered on `axiom-world` + `axiom-packet`. Architecturally reserved now;
  separate spec and plan when started.

## 11. Decisions Log

| Topic | Decision |
|-------|----------|
| Platform / runtime | Paper 1.21.x, Java 21 |
| Build | Gradle multi-module |
| Library nature | Toolkit (math + packet) **+** GrimAPI-style event bus |
| Runtime model | Hybrid: embeddable, or shared singleton when present as a plugin |
| Persistence | Pluggable `StorageProvider`; in-memory + JSON defaults |
| Built-in checks | None — pure toolkit |
| Math priorities | All primitives; physics simulation + base statistics first |
| Packet depth | Approach 2 — transactions + world state tracking + collision |
| Multi-version | Handled via PacketEvents abstraction |
| `axiom-predict` | Designed, deferred to Phase 2 |
