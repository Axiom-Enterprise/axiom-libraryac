# API Reference

A type-by-type tour of the toolkit. Package root: `com.github.axiom.ac`.

---

## axiom-math

Package `com.github.axiom.ac.math`. Pure, dependency-free, deterministic.

### Geometry

- **`Vec3(double x, double y, double z)`** — immutable 3D vector.
  `add`, `subtract`, `scale`, `dot`, `length`, `lengthSquared`, `distance`,
  `distanceSquared`, `normalize`.
- **`Aabb(minX, minY, minZ, maxX, maxY, maxZ)`** — axis-aligned box.
  `intersects(Aabb)` (strict — shared faces do not count), `contains(Vec3)`,
  `expand(dx,dy,dz)`, `offset(dx,dy,dz)`.
- **`Ray(Vec3 origin, Vec3 direction)`** — `intersect(Aabb)` returns an
  `OptionalDouble` distance via the slab method.

### Statistics

- **`Stats`** — `mean`, `variance`, `standardDeviation`, `min`, `max` over
  `double[]` (population definition).
- **`Distribution`** — `skewness`, `kurtosis` (excess), `entropy(long[] counts)`
  (Shannon, in bits). Detects artificial regularity in input.
- **`Gcd`** — `gcd(long,long)`, `gcdOf(long[])`. A large common divisor of
  event intervals signals constant-timed automation.
- **`Outliers`** — `zScore(value, mean, stdDev)`, `percentile(double[], p)`,
  `iqrBounds(double[])`, `isOutlier(value, double[])`.

### Buffers

- **`RollingBuffer<T>(capacity)`** — fixed-capacity FIFO ring; `add`, `get`,
  `size`, `capacity`, `isFull`, `toList`.
- **`SlidingWindow(capacity)`** — double-specialised window over a
  `RollingBuffer`; `add`, `toArray`, `mean`, `standardDeviation`.

### Physics formulas

- **`MotionFormulas`** — pure Minecraft 1.21 movement formulas:
  `nextVerticalVelocity`, `horizontalFriction`, `nextHorizontalVelocity`,
  `jumpVelocity(int boostLevel)`, plus constants (`GRAVITY`, `VERTICAL_DRAG`,
  `AIR_FRICTION`, `DEFAULT_SLIPPERINESS`, `BASE_JUMP_VELOCITY`).

---

## axiom-api

Package `com.github.axiom.ac.api`. The contracts you compile against.

- **`Violation(String checkId, String description, double value, double confidence)`**
  — a detection result. `confidence` is validated to `[0, 1]`.
- **`Check`** — SPI: `String id()`, `Optional<Violation> inspect(PlayerData)`.
- **`PlayerData`** — read-only player view: `uuid`, `position`, `velocity`,
  `yaw`, `pitch`, `onGround`.
- **`StorageProvider`** — SPI: `saveViolation(UUID, Violation)`,
  `loadViolations(UUID)`.
- **`EventBus`** — `channel(Class<T>)` returns the `EventChannel<T>`;
  `publish(event)` routes by runtime class.
- **`EventChannel<T>`** — `subscribe(Consumer<T>)` returns a `Subscription`;
  `publish`, `subscriberCount`.
- **`Subscription`** — `unsubscribe()`.
- **`Cancellable`** — `isCancelled()`, `setCancelled(boolean)`.

Events in `com.github.axiom.ac.api.event`:

- **`FlagEvent`** — `playerId`, `violation`; `Cancellable` (cancel to suppress
  persistence / default consequence).
- **`CheckFaultEvent`** — `checkId`, `reason`; fired when a check is
  auto-disabled.
- **`PlayerJoinEvent`** / **`PlayerQuitEvent`** — `playerId`.

---

## axiom-packet

Package `com.github.axiom.ac.packet`.

- **`MovementUpdate`** — one decoded movement packet. Factories: `full`,
  `positionOnly`, `rotationOnly`, `groundOnly`.
- **`PlayerDataImpl`** — concrete mutable `PlayerData`; `applyMovement`,
  `previousPosition`, `positionHistory`. Thread-confined.
- **`PlayerRegistry`** — `register`, `unregister`, `get`, `all`, `size`.
- **`TransactionManager(TransactionSink)`** — latency compensation:
  `sendTransaction(now)`, `confirm(id, now)` (returns round-trip),
  `isPending`, `pendingCount`, `lastRoundTrip`.
- **`TransactionSink`** — SPI for actually sending a transaction packet.
- **`PacketPipeline`** — the PacketEvents `PacketListenerAbstract` glue.

---

## axiom-world

Package `com.github.axiom.ac.world`. Pure, dependency-free.

- **`BlockPos(int x, int y, int z)`** — block coordinate; `of(Vec3)` floors a
  position.
- **`BlockState`** — `SOLID`, `PASSABLE`, `UNKNOWN` (uncached / desynced).
- **`WorldCache`** — `setBlock(BlockPos, BlockState)`, `blockAt`, `isSolid`,
  `clear`, `size`. **You must populate it** from block updates for collision to
  be meaningful.
- **`CollisionEngine(WorldCache)`** — `collides(Aabb)` (any solid block
  overlapped?), `raycast(Ray, maxDistance)` (Amanatides–Woo voxel traversal,
  returns the first solid `BlockPos`).
- **`PhysicsSimulator(CollisionEngine)`** — `simulate(Aabb box, Vec3 velocity,
  boolean onGround)` returns a `Result(Aabb box, Vec3 velocity, boolean
  onGround)`: applies gravity + friction, then resolves collisions axis by
  axis. Every solid block is treated as a full unit cube.

---

## axiom-core

Package `com.github.axiom.ac.core`.

- **`MemoryStorageProvider`** — in-memory `StorageProvider` (the default).
- **`JsonStorageProvider(Path)`** — JSON-file `StorageProvider`; atomic
  writes, recovers prior violations on construction.
- **`CheckRegistry(EventBus[, faultThreshold])`** — `register`, `unregister`,
  `isRegistered`, `size`, `inspect(PlayerData)`. Isolates throwing checks and
  auto-disables one after `faultThreshold` consecutive faults.
- **`AxiomRuntime(StorageProvider)`** — the central object. Accessors:
  `eventBus`, `players`, `world`, `collision`, `checks`, `storage`. Lifecycle:
  `handlePlayerJoin(uuid)`, `handlePlayerQuit(uuid)`, `inspect(uuid)`.
- **`AxiomProvider`** — static shared-runtime holder: `set`, `get`, `clear`.

---

## axiom-predict

Package `com.github.axiom.ac.predict`. The movement-prediction engine.

- **`MovementInput(int forward, int strafe, boolean jump, boolean sprint)`** —
  one candidate set of client inputs; axes in `{-1, 0, 1}`. `none()`.
- **`InputSpace`** — `all()` returns the 36 candidate inputs.
- **`PlayerState(Vec3 position, Vec3 velocity, float yaw, boolean onGround)`** —
  a movement snapshot.
- **`PredictionEngine(PhysicsSimulator)`** — `predict(PlayerState,
  MovementInput)` returns the next `PlayerState`.
- **`PredictionResult(MovementInput input, PlayerState predicted, double offset)`**.
- **`MovementPredictor(PredictionEngine)`** — `bestPrediction(PlayerState
  previous, Vec3 actualPosition)` searches every input and returns the closest
  match. The `offset` is the cheat signal: near zero for legitimate play,
  large when no legitimate input explains the move.

> **Accuracy note.** The `PredictionEngine` movement constants are a documented
> *baseline approximation* — Minecraft's `moveRelative` direction math with
> plausible constants, not yet tuned to a specific game version. The engine and
> search are exact; treat the offset as a relative signal until the constants
> are calibrated. This is acknowledged future work.

---

## Conventions

- All value types are immutable `record`s; data structures are `final` classes.
- Constructors and setters reject `null` with `Objects.requireNonNull`.
- Methods return `Optional` / `OptionalDouble` / `OptionalLong` rather than
  `null`.
- "Not thread-safe" in a Javadoc means thread-confined — use it on one thread.
