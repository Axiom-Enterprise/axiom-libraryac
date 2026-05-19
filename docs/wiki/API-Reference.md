# API Reference

A type-by-type tour of the toolkit. Package root: `com.github.axiom.ac`.

---

## axiom-math

Package `com.github.axiom.ac.math`. Pure, dependency-free, deterministic.

### Geometry

- **`Vec3(double x, double y, double z)`** — immutable 3D vector.
  `add`, `subtract`, `scale`, `dot`, `length`, `lengthSquared`, `distance`,
  `distanceSquared`, `normalize`, `clampLength(max)` (shorten to a maximum
  magnitude without changing direction); constant `Vec3.ZERO`.
- **`Aabb(minX, minY, minZ, maxX, maxY, maxZ)`** — axis-aligned box.
  `intersects(Aabb)` (strict — shared faces do not count), `contains(Vec3)`,
  `expand(dx,dy,dz)`, `offset(dx,dy,dz)`, `closestPoint(Vec3)`,
  `distanceTo(Vec3)`, `distanceSquaredTo(Vec3)` (0 when the point is inside).
- **`Ray(Vec3 origin, Vec3 direction)`** — `intersect(Aabb)` returns an
  `OptionalDouble` distance via the slab method.
- **`Rotation(float yaw, float pitch)`** — an immutable look angle, in
  degrees. `directionVector()` (unit look vector), `wrapDegrees(double)`,
  `normalized()` (yaw wrapped to `[-180, 180)`, pitch clamped to `[-90, 90]`),
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

### Normalization

- **`Normalizer`** — generic scalar normalization, the shared layer that turns
  open-ended measurements into bounded, comparable scores: `clamp(value, min,
  max)`, `clampUnit(value)`, `minMax(value, min, max)` (linear map onto
  `[0, 1]`), `zScore(value, mean, stdDev)`, `sigmoid(value)`, and
  `softScore(value, midpoint, steepness)` (a graded `[0, 1]` confidence).

### Physics formulas

- **`MotionFormulas`** — pure Minecraft 1.21 movement formulas:
  `nextVerticalVelocity`, `nextVerticalVelocitySlowFalling`,
  `nextVerticalVelocityInFluid(velocityY, drag)`,
  `nextVerticalVelocityLevitation(velocityY, level)`, `horizontalFriction`,
  `nextHorizontalVelocity`, `jumpVelocity(int boostLevel)`,
  `groundAcceleration(friction, sprint)` (input acceleration scaled by the
  inverse cube of the surface friction), `airAcceleration(sprint)`,
  `riptideLaunchSpeed(level)`, `depthStriderWaterFriction(level, onGround)`,
  `depthStriderWaterAcceleration(level, onGround)`, plus
  constants for gravity, drag, friction, fluids (`WATER_DRAG`, `LAVA_DRAG`,
  `FLUID_GRAVITY`, `FLUID_ACCELERATION`), climbing (`CLIMB_UP_SPEED`,
  `CLIMB_FALL_CLAMP`, `CLIMB_HORIZONTAL_CLAMP`), cobweb, levitation, slow
  falling, `STEP_HEIGHT`, and the 1.21 flight / fluid mechanics — firework
  elytra boost (`FIREWORK_LOOK_GAIN`, `FIREWORK_TARGET_FACTOR`,
  `FIREWORK_CONVERGENCE`), Riptide, bubble columns
  (`BUBBLE_COLUMN_UP_*`, `BUBBLE_COLUMN_DOWN_*`), powder snow
  (`POWDER_SNOW_DRAG_HORIZONTAL`, `POWDER_SNOW_DESCENT_CLAMP`), Depth Strider,
  and Dolphin's Grace (`DOLPHINS_GRACE_FRICTION`).

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
- **`MovementUpdateNormalizer`** — fills the partial packets a client sends
  into a stream of complete ones: `normalize(MovementUpdate)` carries the last
  known position and look angle forward over a packet that omits them, and
  canonicalises the rotation. `hasPosition()`, `hasRotation()`, `position()`,
  `rotation()`. One per connection, thread-confined.
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
- **`Fluid`** — `NONE`, `WATER`, `LAVA`: the fluid a block contributes.
- **`BubbleColumn`** — `NONE`, `UPWARD`, `DOWNWARD`: the bubble-column current
  a water block carries (above soul sand, or above magma).
- **`BlockState`** — a block's collision shape, surface friction, and material
  traits. The collision shape is a list of cell-local boxes (components in
  `[0, 1]`), canonicalised through `ShapeNormalizer` on `build()`. Constants:
  `SOLID`, `PASSABLE`, `UNKNOWN` (uncached / desynced),
  `ICE` / `PACKED_ICE` / `BLUE_ICE`, `SLIME_BLOCK`, `SOUL_SAND`, `HONEY_BLOCK`,
  `BOTTOM_SLAB` / `TOP_SLAB`, `WATER`, `LAVA`, `LADDER`, `VINE`, `SCAFFOLDING`,
  `COBWEB`, `BUBBLE_COLUMN_UPWARD` / `BUBBLE_COLUMN_DOWNWARD`, `POWDER_SNOW`.
  Factories `cube(name, slipperiness)`, `shape(name, slipperiness,
  Aabb...)`, and `builder(name)` (fluent: `fullCube`, `shape`, `slipperiness`,
  `fluid`, `climbable`, `bouncy`, `cobweb`, `scaffolding`, `powderSnow`,
  `bubbleColumn`, `speedMultiplier`). Queries
  `collisionBoxes()`, `slipperiness()`, `fluid()`, `isClimbable()`,
  `isBouncy()`, `isCobweb()`, `isScaffolding()`, `isPowderSnow()`,
  `bubbleColumn()`, `speedMultiplier()`, `hasCollision()`,
  `isFluid()`, `isPassable()`, `isUnknown()`.
- **`WorldCache`** — `setBlock(BlockPos, BlockState)`, `blockAt`, `isSolid`
  (now "collidable"), `clear`, `size`. **You must populate it** from block
  updates for collision to be meaningful.
- **`CollisionEngine(WorldCache)`** — `collides(Aabb)` (does the box overlap
  any block's collision shape? — the query box is run through `AabbNormalizer`
  and `VoxelNormalizer` first), `raycast(Ray, maxDistance)` (Amanatides–Woo
  voxel traversal, returns the first collidable `BlockPos`), `world()`.
- **`AabbNormalizer`** — canonicalises bounding boxes: `normalize(box)` orders
  inverted axes, `cellLocal(box)` orders and clamps into the unit cell,
  `isCanonical(box)`, `isCellLocal(box)`.
- **`ShapeNormalizer`** — `normalize(List<Aabb>)` reduces a collision shape to
  its minimal canonical form (degenerate, duplicate, and enclosed boxes
  dropped, the rest sorted); `isCanonical(List<Aabb>)`.
- **`VoxelNormalizer`** — raw coordinates to voxels: `blockAt(Vec3[, epsilon])`
  snaps near-integer float drift to the boundary before flooring, `snap`,
  `chunkLocal(int)` / `chunkLocal(BlockPos)`, `chunkOf(int)`.
- **`PhysicsSimulator(CollisionEngine)`** — a `record`; two layers.
  `move(box, velocity, onGround, stepAssist)` is the pure collision mover:
  axis-by-axis resolution plus, when `stepAssist` is set and the player is
  grounded, stepping up ledges no taller than `STEP_HEIGHT`.
  `simulate(...)` adds the walking-physics velocity update (friction decay —
  ground `slipperiness * 0.91`, air bare `0.91` — then input acceleration) and
  calls `move`; overloads `simulate(box, velocity, onGround)`,
  `simulate(box, velocity, inputAcceleration, onGround)`, and
  `simulate(box, velocity, inputAcceleration, onGround, slipperiness)`. Also
  `collision()`, `slipperinessBelow(box)`, `supportingBlock(box)`. Every result
  is a `Result(Aabb box, Vec3 velocity, boolean onGround)`. Shaped blocks
  (slabs) collide with only their occupied sub-cell.

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

- **`MovementInput(int forward, int strafe, boolean jump, boolean sprint,
  boolean sneak)`** — one candidate set of client inputs; axes in `{-1, 0, 1}`.
  A four-argument constructor leaves sneak unpressed. `none()`,
  `isSprintingForward()` (sprint only counts with forward held),
  `hasDirectionalInput()`.
- **`MovementContext(int jumpBoost, int speedAmplifier, int slownessAmplifier,
  int levitationLevel, boolean slowFalling, boolean elytra, boolean
  fireworkBoost, int riptideLevel, int depthStrider, boolean dolphinsGrace)`**
  — the slowly changing player attributes a prediction depends on: potion
  effects, worn enchantments, and the elytra / firework / Riptide flight
  state. A six-argument constructor leaves the flight extras neutral. `none()`,
  `builder()` (fluent), `speedEffectMultiplier()`, `hasLevitation()`,
  `hasRiptideLaunch()`.
- **`InputSpace`** — `all()` returns the 72 candidate inputs (forward × strafe
  × jump × sprint × sneak).
- **`PlayerState(Vec3 position, Vec3 velocity, float yaw, float pitch,
  boolean onGround)`** — a movement snapshot; `rotation()`. A four-argument
  constructor leaves pitch level.
- **`PredictionEngine(PhysicsSimulator)`** — `predict(PlayerState,
  MovementInput[, MovementContext])` returns the next `PlayerState`. It selects
  a movement branch per tick — walking/air, water, lava, powder snow,
  climbing, or elytra — and applies sprint rules, sneak slowdown, Jump Boost,
  the sprint-jump impulse, Speed/Slowness, Levitation, Slow Falling, soul-sand
  and honey slowdown, slime bounce, cobweb entanglement, elytra firework
  boost, the Riptide launch impulse, water Depth Strider / Dolphin's Grace,
  bubble-column currents, and scaffolding sneak-descent.
- **`PredictionResult(MovementInput input, PlayerState predicted, double offset)`**.
- **`MovementPredictor(PredictionEngine)`** — `bestPrediction(PlayerState
  previous, Vec3 actualPosition[, MovementContext])` searches every input and
  returns the closest match. The `offset` is the cheat signal: near zero for
  legitimate play, large when no legitimate input explains the move.
- **`OffsetNormalizer(double noiseFloor, double saturation)`** — maps a raw
  prediction `offset` to a bounded `[0, 1]` cheat score: `score(offset)` /
  `score(PredictionResult)` (0 up to the noise floor, rising linearly to 1 at
  saturation), `isSuspicious(PredictionResult)`. Constant `DEFAULT`
  (3&nbsp;cm floor, half-block saturation).

> **Accuracy note.** The `PredictionEngine` reproduces Minecraft's branched
> `travel` model — branch selection (elytra, water, lava, powder snow,
> climbable, walking), tick ordering (friction/medium decay, then input
> acceleration, then axis-by-axis collision with step assistance), and the
> environmental and effect modifiers — exactly. The movement *constants* are a
> documented 1.21+ *baseline approximation*, not yet calibrated bit-for-bit to
> a specific game version — treat the offset as a relative signal, and feed it
> through `OffsetNormalizer` for a graded score. Final constant calibration is
> acknowledged future work.

---

## Conventions

- All value types are immutable `record`s; data structures are `final` classes.
- Constructors and setters reject `null` with `Objects.requireNonNull`.
- Methods return `Optional` / `OptionalDouble` / `OptionalLong` rather than
  `null`.
- "Not thread-safe" in a Javadoc means thread-confined — use it on one thread.
