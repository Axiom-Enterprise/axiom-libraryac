# For AI Agents

A dense, structured map of Axiom AC for automated tools and AI assistants
working with this codebase. Human-oriented prose is in the other wiki pages;
this page is the quick-reference.

## What this project is

A Minecraft (Paper) anticheat **toolkit library**. It provides no detection
checks — it is the framework (`Check` SPI, math, packet pipeline, world model,
physics, prediction, event bus) for building one.

## Facts

| Key | Value |
|-----|-------|
| Language / toolchain | Java 21 |
| Build | Gradle multi-module (wrapper included) |
| Group | `com.github.axiom.ac` |
| Version | `1.0-SNAPSHOT` |
| Package root | `com.github.axiom.ac` |
| Modules | `axiom-math`, `axiom-api`, `axiom-packet`, `axiom-world`, `axiom-core`, `axiom-plugin`, `axiom-predict` |
| External deps | PacketEvents 2.12.1 (`compileOnly`), Paper API 1.21.x (`compileOnly`), Gson 2.11.0 |
| Tests | 226 unit tests (JUnit 6), all passing |
| Published artifacts | none — build from source |

## Build / test commands

```bash
./gradlew build                       # all modules, compile + test
./gradlew test                        # full test suite
./gradlew :axiom-math:test            # one module's tests
./gradlew :<module>:compileJava       # compile-check one module
```

Windows: use `.\gradlew.bat`. The shell in this environment is PowerShell.

## Module map (one line each)

- `axiom-math` — `Vec3`, `Aabb`, `Ray`, `Rotation`, `Stats`, `Distribution`,
  `Gcd`, `Outliers`, `RollingBuffer`, `SlidingWindow`, `MotionFormulas`,
  `CombatMath`, `AimAnalysis`. No deps.
- `axiom-api` — `Check`, `PlayerData` (with rotation history), `Violation`,
  `StorageProvider`, `EventBus`, `EventChannel`, `Subscription`, `Cancellable`;
  events `FlagEvent`, `CheckFaultEvent`, `PlayerJoinEvent`, `PlayerQuitEvent`.
- `axiom-packet` — `MovementUpdate`, `PlayerDataImpl` (position + rotation
  history), `PlayerRegistry`, `TransactionManager` (RTT smoothing + jitter),
  `TransactionSink`, `PacketPipeline` (optional movement listener).
- `axiom-world` — `BlockPos`, `BlockState` (collision shape + slipperiness),
  `WorldCache`, `CollisionEngine`, `PhysicsSimulator`. No deps.
- `axiom-core` — `MemoryStorageProvider`, `JsonStorageProvider`,
  `CheckRegistry`, `AxiomRuntime`, `AxiomProvider`.
- `axiom-plugin` — `AxiomPlugin` (Paper `JavaPlugin`), `plugin.yml`.
- `axiom-predict` — `MovementInput`, `InputSpace`, `PlayerState`,
  `PredictionEngine`, `PredictionResult`, `MovementPredictor`. Deps:
  `axiom-math`, `axiom-world`.

## Dependency order (compile leaves first)

`axiom-math` → `axiom-api` → `axiom-packet` → `axiom-world` → `axiom-core` →
`axiom-plugin`. `axiom-predict` depends on `axiom-math` + `axiom-world` only.

## The one workflow that matters

To add detection logic:

1. Implement `com.github.axiom.ac.api.Check` — `id()` and
   `Optional<Violation> inspect(PlayerData)`.
2. `runtime.checks().register(check)` on an `AxiomRuntime`.
3. Subscribe: `runtime.eventBus().channel(FlagEvent.class).subscribe(...)`.
4. `runtime.inspect(uuid)` runs the checks. Under `axiom-plugin` it is wired to
   fire per movement packet (netty thread); a standalone embedder schedules it.

`AxiomRuntime` is reached via `AxiomProvider.get()` (when the plugin is
running) or constructed directly: `new AxiomRuntime(new MemoryStorageProvider())`.
Reach/aim checks: build on `CombatMath` (eye, hitbox, reach, line of sight) and
`AimAnalysis` (rotation deltas, snaps, sensitivity GCD) over
`PlayerData.rotationHistory()`.

## Conventions to follow when editing

- Value types are immutable `record`s; structures are `final` classes.
- Constructors/setters use `Objects.requireNonNull(x, "name")`.
- Methods return `Optional`/`OptionalDouble`/`OptionalLong`, never `null`.
- Modern JDK 21 idioms — no legacy getter/setter boilerplate.
- TDD: a test exists for every non-glue type. Add a failing test first.
- Per the repository's commit policy: commit author is
  `Carlo Maria Cardí <112445885+MathsAnalysis@users.noreply.github.com>`;
  **never** add an AI `Co-Authored-By` trailer; commit messages in English,
  conventional-commit style.

## Threading rules

- `PlayerDataImpl`, `TransactionManager` — thread-confined to one netty thread,
  no synchronization. Do not share across threads.
- `PlayerRegistry`, `WorldCache`, `CheckRegistry`, storage providers — concurrent.
- A `Check` runs on a packet thread — it must not call the Bukkit API.

## Known limitations / future work

- `axiom-predict` movement constants are a 1.21 baseline approximation, not
  calibrated bit-for-bit — the offset is a relative signal. Tick ordering and
  the search are exact.
- `axiom-world` models full cubes and slabs (`BlockState.shape`); shapes that
  exceed a unit cell (fences, walls) are clipped to the cell.
- PacketEvents/Paper glue (`PacketPipeline`, `AxiomPlugin`) compiles against the
  real APIs but is not exercised by unit tests — verify on a live server.
- `JsonStorageProvider.saveViolation` writes the file synchronously on the
  calling (netty) thread — batch or offload it before heavy production load.

## Where the design lives

- Spec: `docs/superpowers/specs/2026-05-18-axiom-ac-design.md`
- Implementation plans: `docs/superpowers/plans/2026-05-18-axiom-ac-*.md`
  (6 plans, one per module group, each with TDD task breakdown).
