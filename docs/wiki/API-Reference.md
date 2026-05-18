# API Reference

A type-by-type tour of the toolkit. Package root: `com.github.axiom.ac`.

---

## axiom-math

Package `com.github.axiom.ac.math`. Pure, dependency-free, deterministic.

### Geometry

- **`Vec3(double x, double y, double z)`** — immutable 3D vector.
  `add`, `subtract`, `scale`, `dot`, `length`, `lengthSquared`, `distance`,
  `distanceSquared`, `normalize`; constant `Vec3.ZERO`.
- **`Aabb(minX, minY, minZ, maxX, maxY, maxZ)`** — axis-aligned box.
  `intersects(Aabb)` (strict — shared faces do not count), `contains(Vec3)`,
  `expand(dx,dy,dz)`, `offset(dx,dy,dz)`, `closestPoint(Vec3)`,
  `distanceTo(Vec3)`, `distanceSquaredTo(Vec3)` (0 when the point is inside).
- **`Ray(Vec3 origin, Vec3 direction)`** — `intersect(Aabb)` returns an
  `OptionalDouble` distance via the slab method.
- **`Rotation(float yaw, float pitch)`** — an immutable look angle, in
  degrees. `directionVector()` (unit look vector), `wrapDegrees(double)`,
  `yawDelta(Rotation)` / `pitchDelta(Rotation)` (signed, yaw wrapped to the
  shortest turn), `magnitudeDelta(Rotation)`, `angleTo(Rotation)`.

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
  `jumpVelocity(int boostLevel)`, `groundAcceleration(friction, sprint)`
  (input acceleration scaled by the inverse cube of the surface friction),
  `airAcceleration(sprint)`, plus constants (`GRAVITY`, `VERTICAL_DRAG`,
  `AIR_FRICTION`, `DEFAULT_SLIPPERINESS`, `BASE_JUMP_VELOCITY`, `WALK_SPEED`,
  `SPRINT_MULTIPLIER`, `GROUND_ACCELERATION_CONSTANT`, `AIR_ACCELERATION`).

### Reach & aim

- **`CombatMath`** — combat geometry. `eyePosition(feet[, eyeHeight])`,
  `playerHitbox(feet)` / `hitbox(feet, width, height)`,
  `reachDistance(eye, targetHitbox)` (eye to nearest hitbox point),
  `withinReach(eye, target, maxReach)`,
  `lineOfSightDistance(eye, Rotation, target)` (crosshair-ray hit distance,
  `OptionalDouble`), `looksAt(eye, Rotation, target, maxDistance)`. Constants
  `DEFAULT_EYE_HEIGHT`, `SNEAKING_EYE_HEIGHT`, `PLAYER_WIDTH`, `PLAYER_HEIGHT`.
- **`AimAnalysis`** — rotation-pattern analysis over a `List<Rotation>`
  (oldest first): `yawDeltas`, `pitchDeltas`, `angularChanges`,
  `maxAngularChange`, `hasSnap(samples, snapDegrees)`,
  `quantizationGcd(double[] deltas, double scale)` — the discrete mouse step
  underlying the samples; a divisor that collapses toward the rounding unit
  signals fractional, non-quantised corrections. Pair with `Stats` / `Outliers`.

---

## axiom-api

Package `com.github.axiom.ac.api`. The contracts you compile against.

- **`Violation(String checkId, String description, double value, double confidence)`**
  — a detection result. `confidence` is validated to `[0, 1]`.
- **`Check`** — SPI: `String id()`, `Optional<Violation> inspect(PlayerData)`.
- **`PlayerData`** — read-only player view: `uuid`, `position`, `velocity`,
  `yaw`, `pitch`, `onGround`, plus `rotation()`, `previousRotation()` and
  `rotationHistory()` (default methods; the packet-pipeline implementation
  overrides them with a genuine bounded history).
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
  `previousPosition`, `positionHistory`, plus rotation tracking:
  `rotationHistory()`, `rotationBuffer()`, `previousRotation()`,
  `lastYawDelta()`, `lastPitchDelta()`. Thread-confined.
- **`PlayerRegistry`** — `register`, `unregister`, `get`, `all`, `size`.
- **`TransactionManager(TransactionSink)`** — latency compensation:
  `sendTransaction(now)`, `confirm(id, now)` (returns round-trip),
  `isPending`, `pendingCount`, `lastRoundTrip`, plus `smoothedRoundTrip()`
  (spike-resistant EWMA latency) and `jitter()` (EWMA of sample deviation).
- **`TransactionSink`** — SPI for actually sending a transaction packet.
- **`PacketPipeline`** — the PacketEvents `PacketListenerAbstract` glue.
  The `PacketPipeline(PlayerRegistry, Consumer<UUID>)` constructor takes a
  movement listener invoked after each tracked player's data is updated; the
  runtime wires it to its inspection pass.

---

## axiom-world

Package `com.github.axiom.ac.world`. Pure, dependency-free.

- **`BlockPos(int x, int y, int z)`** — block coordinate; `of(Vec3)` floors a
  position.
- **`BlockState`** — a block's collision shape and surface friction. The
  collision shape is a list of cell-local boxes (components in `[0, 1]`).
  Constants: `SOLID`, `PASSABLE`, `UNKNOWN` (uncached / desynced),
  `ICE` / `PACKED_ICE` / `BLUE_ICE`, `SLIME_BLOCK`, `BOTTOM_SLAB` / `TOP_SLAB`.
  Factories `cube(name, slipperiness)` and `shape(name, slipperiness, Aabb...)`.
  Queries `collisionBoxes()`, `slipperiness()`, `hasCollision()`,
  `isPassable()`, `isUnknown()`.
- **`WorldCache`** — `setBlock(BlockPos, BlockState)`, `blockAt`, `isSolid`
  (now "collidable"), `clear`, `size`. **You must populate it** from block
  updates for collision to be meaningful.
- **`CollisionEngine(WorldCache)`** — `collides(Aabb)` (does the box overlap
  any block's collision shape?), `raycast(Ray, maxDistance)` (Amanatides–Woo
  voxel traversal, returns the first collidable `BlockPos`), `world()`.
- **`PhysicsSimulator(CollisionEngine)`** — `simulate(...)` returns a
  `Result(Aabb box, Vec3 velocity, boolean onGround)`. One tick, in
  Minecraft's order: friction decay (ground friction is `slipperiness * 0.91`,
  air friction is the bare `0.91`), then the caller's input acceleration, then
  axis-by-axis collision resolution. Overloads: `simulate(box, velocity,
  onGround)`, `simulate(box, velocity, inputAcceleration, onGround)` (both read
  the surface slipperiness from the block below), and `simulate(box, velocity,
  inputAcceleration, onGround, slipperiness)`. `collision()` exposes the
  engine. Shaped blocks (slabs) collide with only their occupied sub-cell.

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

> **Accuracy note.** The `PredictionEngine` tick ordering matches Minecraft's
> `travel`: friction decay (ground vs air, surface slipperiness read from the
> block beneath the player), then `moveRelative` input acceleration (scaled by
> the inverse cube of the surface friction on the ground, a fixed value in the
> air), then axis-by-axis collision. The ordering and search are exact; the
> movement constants are a documented 1.21 *baseline approximation*, not yet
> calibrated bit-for-bit to a specific game version — treat the offset as a
> relative signal. Final constant calibration is acknowledged future work.

---

## Conventions

- All value types are immutable `record`s; data structures are `final` classes.
- Constructors and setters reject `null` with `Objects.requireNonNull`.
- Methods return `Optional` / `OptionalDouble` / `OptionalLong` rather than
  `null`.
- "Not thread-safe" in a Javadoc means thread-confined — use it on one thread.
