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

## axiom-detect

Package root `com.github.axiom.ac.detect`. The check-building toolkit: one
subpackage per concern. Ships no concrete checks — it is the scaffolding a
check author extends. Depends on `axiom-api`, `axiom-math`, `axiom-world`,
`axiom-predict`.

### Per-player state — `detect.session`

- **`SessionStore<S>`** — concurrent, UUID-keyed store of per-player state, so a
  stateless `Check` can keep memory between inspections. `getOrCreate(uuid,
  factory)`, `get(uuid)`, `put`, `remove`, `clear`, `size`. Each value is
  thread-confined to its player; the map handles join/quit bookkeeping.

### Confidence — `detect.signal`

- **`Confidence`** — maps a raw magnitude onto the `[0, 1]` a `Violation`
  requires: `clamp(v)`, `ramp(value, floor, ceiling)` (0 at the floor → 1 at the
  ceiling), `saturating(value, scale)`.

### Heuristic checks — `detect.heuristic`

- **`ViolationLevel`** — mutable, non-negative suspicion accumulator: `add`,
  `decay` (floored at 0), `level`, `reset`. Thread-confined.
- **`HeuristicSignal(boolean failed, double weight, String detail)`** — the
  outcome of one evaluation. `pass()`, `fail(weight, detail)`.
- **`AbstractHeuristicCheck(id, flagThreshold, saturationLevel, decayPerPass)`**
  — base `Check` for the threshold-with-decay pattern. Implement
  `evaluate(PlayerData)` → `HeuristicSignal`; the base accumulates weights into a
  per-player `ViolationLevel`, decays it on clean ticks, and flags once the level
  crosses the threshold, with confidence ramped toward the saturation level.
  `forget(uuid)`, `reset()`.

### Statistical checks — `detect.statistical`

- **`StatisticalCriterion`** — SPI: `OptionalDouble score(double[] samples)`.
  Empty means "not enough data"; a higher score is more anomalous. Units are
  criterion-defined, so pair each with a matching threshold.
- **`ZScoreCriterion(minSamples)`** — absolute z-score of the latest sample
  against the window.
- **`IqrCriterion(minSamples)`** — how far the latest sample escapes the Tukey
  IQR fences, in IQRs (0 when inside); robust to the very outliers it hunts.
- **`RegularityCriterion(buckets, minSamples)`** — entropy deficit
  `log2(buckets) − H` over a histogram of the window; high for artificially
  regular input.
- **`PeriodicityCriterion(minSamples)`** — GCD of the rounded interval samples;
  the score is the common period, so the threshold is "smallest suspicious
  period".
- **`SampleAccumulator(windowSize)`** — per-player sliding windows:
  `record(uuid, value)` returns the retained samples, `mean(uuid)`, `forget`,
  `reset`.
- **`AbstractStatisticalCheck(id, criterion, windowSize, flagScore, saturationScore)`**
  — base `Check`: implement `sample(PlayerData)` → `OptionalDouble`; the base
  feeds a per-player window, scores it with the criterion, and flags above the
  flag score with confidence ramped toward the saturation score. `forget(uuid)`,
  `reset()`.

### Raytrace engine — `detect.raytrace`

- **`LookVectors`** — Minecraft look math: `direction(yaw, pitch)` (unit vector,
  matching `Location.getDirection()`), `eyePosition(feet, eyeHeight)`; constants
  `STANDING_EYE_HEIGHT`, `SNEAKING_EYE_HEIGHT`.
- **`Hitbox<T>(T target, Aabb box)`** — a target box plus the caller's opaque
  handle for what it represents.
- **`RayHit<T>(Hitbox<T> hitbox, double distance, Vec3 point)`** — an
  intersection; `distance` and `point` are world-space.
- **`RaytraceEngine(CollisionEngine)`** — `nearest(ray, maxDistance, targets)`
  (closest hitbox struck, ignoring occlusion) and `nearestVisible(eye,
  direction, maxDistance, targets)` (closest hitbox with a clear line of sight).
- **`ReachResolver`** — combat reach: `distance(eye, direction, target)` and
  `minimumDistance(eyePositions, direction, target)` (the latency-compensated
  smallest reach across candidate eye positions). Expand the target box first for
  hitbox tolerance.
- **`LineOfSight`** — `clear(from, to, collision)`: true when no cached solid
  block lies between the points (whole-block resolution).

### Prediction probe — `detect.prediction`

- **`PredictionProbe(MovementPredictor, windowSize)`** — drives the stateless
  predictor from a live state stream. `observe(uuid, PlayerState)` returns the
  prediction offset against the previous observation (0 on the first);
  `averageOffset(uuid)` smooths it over a rolling window; `forget(uuid)`. The
  bridge from `axiom-predict` into the heuristic/statistical check bases.

---

## Conventions

- All value types are immutable `record`s; data structures are `final` classes.
- Constructors and setters reject `null` with `Objects.requireNonNull`.
- Methods return `Optional` / `OptionalDouble` / `OptionalLong` rather than
  `null`.
- "Not thread-safe" in a Javadoc means thread-confined — use it on one thread.
