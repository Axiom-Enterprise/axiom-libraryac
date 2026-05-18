# Axiom AC — World Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `axiom-world`, the server-side world model: a block cache, a collision engine (AABB-vs-world and voxel raycasting), and a collision-aware physics simulator.

**Architecture:** A Gradle module depending only on `axiom-math`. It is pure, deterministic, dependency-free (no PacketEvents, no Paper) and fully unit-tested. The cache is populated by callers (the plugin glue in a later plan feeds it block updates); this module only provides the data structure and the math over it.

**Tech Stack:** Java 21, Gradle (multi-module), JUnit 6.

This plan is **Plan 4 of 5**. It implements `axiom-world` from `docs/superpowers/specs/2026-05-18-axiom-ac-design.md` (section 4.4). Plans 1-3 are merged.

**Spec refinement:** the design spec's dependency graph listed `world → api+math+packet`. This plan makes `axiom-world` depend only on `axiom-math`. Rationale: the collision engine and physics simulator operate on raw geometry (`Vec3`, `Aabb`, `Ray`), not on `PlayerData` — keeping the module dependency-light makes it fully unit-testable and reusable. Callers adapt `PlayerData` into geometry at the call site.

**Block model:** a cached block is `SOLID` (full unit-cube collision), `PASSABLE` (no collision), or `UNKNOWN` (not cached — world desync). Partial block shapes (slabs, stairs) are out of scope for this module version; every solid block is treated as a full cube.

---

## File Structure

```
axiom-libraryac/
├── settings.gradle                       MODIFY — add axiom-world module
├── axiom-world/
│   ├── build.gradle                      CREATE — depends on axiom-math
│   └── src/
│       ├── main/java/com/github/axiom/ac/world/
│       │   ├── BlockPos.java              Integer block coordinate
│       │   ├── BlockState.java            SOLID / PASSABLE / UNKNOWN
│       │   ├── WorldCache.java            Block-state cache
│       │   ├── CollisionEngine.java       AABB-vs-world + voxel raycast
│       │   └── PhysicsSimulator.java      Collision-aware tick prediction
│       └── test/java/com/github/axiom/ac/world/
│           └── (test classes per the tasks below)
```

**Windows note:** commands use `.\gradlew.bat` (PowerShell).

---

## Task 1: Add the axiom-world module

**Files:**
- Modify: `settings.gradle`
- Create: `axiom-world/build.gradle`

- [ ] **Step 1: Replace `settings.gradle`**

```groovy
rootProject.name = 'axiom-libraryac'

include 'axiom-math'
include 'axiom-api'
include 'axiom-packet'
include 'axiom-world'
```

- [ ] **Step 2: Create `axiom-world/build.gradle`**

```groovy
// axiom-world: server-side world model — block cache, collision
// engine, physics simulator. Pure geometry math; depends only on
// axiom-math. Build configuration is inherited from the root.
dependencies {
    implementation project(':axiom-math')
}
```

- [ ] **Step 3: Verify the module is recognised**

Run: `.\gradlew.bat projects`
Expected: output lists `Project ':axiom-world'` alongside the others.

- [ ] **Step 4: Commit**

```bash
git add settings.gradle axiom-world/build.gradle
git commit -m "build: add axiom-world module depending on axiom-math"
```

---

## Task 2: BlockState — block collision state

**Files:**
- Create: `axiom-world/src/main/java/com/github/axiom/ac/world/BlockState.java`

A plain enum, verified by compilation; later tasks exercise it.

- [ ] **Step 1: Create `BlockState.java`**

```java
package com.github.axiom.ac.world;

/**
 * Collision state of a cached block.
 */
public enum BlockState {

    /** Full unit-cube collision. */
    SOLID,

    /** No collision — the player passes through. */
    PASSABLE,

    /** Not cached. The world is desynced here; checks must decide
     *  how to treat the uncertainty. */
    UNKNOWN
}
```

- [ ] **Step 2: Verify it compiles**

Run: `.\gradlew.bat :axiom-world:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add axiom-world/src/main/java/com/github/axiom/ac/world/BlockState.java
git commit -m "feat: add BlockState enum to axiom-world"
```

---

## Task 3: BlockPos — integer block coordinate

**Files:**
- Create: `axiom-world/src/main/java/com/github/axiom/ac/world/BlockPos.java`
- Test: `axiom-world/src/test/java/com/github/axiom/ac/world/BlockPosTest.java`

An immutable integer (x, y, z) block coordinate. `of(Vec3)` floors a world position to the block containing it.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.axiom.ac.math.Vec3;
import org.junit.jupiter.api.Test;

class BlockPosTest {

    @Test
    void exposesCoordinates() {
        BlockPos pos = new BlockPos(1, 2, 3);
        assertEquals(1, pos.x());
        assertEquals(2, pos.y());
        assertEquals(3, pos.z());
    }

    @Test
    void ofFloorsPositiveCoordinates() {
        assertEquals(new BlockPos(1, 64, 2), BlockPos.of(new Vec3(1.7, 64.9, 2.1)));
    }

    @Test
    void ofFloorsNegativeCoordinates() {
        assertEquals(new BlockPos(-2, -1, -3), BlockPos.of(new Vec3(-1.3, -0.1, -2.5)));
    }

    @Test
    void ofIsExactOnIntegerBoundaries() {
        assertEquals(new BlockPos(5, 0, -7), BlockPos.of(new Vec3(5.0, 0.0, -7.0)));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-world:test --tests "com.github.axiom.ac.world.BlockPosTest"`
Expected: FAIL — compilation error, `BlockPos` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.world;

import com.github.axiom.ac.math.Vec3;

/**
 * Immutable integer block coordinate.
 *
 * @param x block x
 * @param y block y
 * @param z block z
 */
public record BlockPos(int x, int y, int z) {

    /**
     * Returns the block that contains world position {@code v},
     * flooring each component.
     */
    public static BlockPos of(Vec3 v) {
        return new BlockPos(
                (int) Math.floor(v.x()),
                (int) Math.floor(v.y()),
                (int) Math.floor(v.z()));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-world:test --tests "com.github.axiom.ac.world.BlockPosTest"`
Expected: PASS — 4 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-world/src/main/java/com/github/axiom/ac/world/BlockPos.java axiom-world/src/test/java/com/github/axiom/ac/world/BlockPosTest.java
git commit -m "feat: add BlockPos block coordinate to axiom-world"
```

---

## Task 4: WorldCache — block-state cache

**Files:**
- Create: `axiom-world/src/main/java/com/github/axiom/ac/world/WorldCache.java`
- Test: `axiom-world/src/test/java/com/github/axiom/ac/world/WorldCacheTest.java`

Stores the known collision state of blocks. An uncached block reads back as `UNKNOWN`. Setting a block to `UNKNOWN` forgets it. Backed by a `ConcurrentHashMap`: block updates arrive on the server thread, lookups happen on netty threads.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorldCacheTest {

    @Test
    void uncachedBlockIsUnknown() {
        assertEquals(BlockState.UNKNOWN, new WorldCache().blockAt(new BlockPos(0, 0, 0)));
    }

    @Test
    void storedBlockIsReadBack() {
        WorldCache cache = new WorldCache();
        BlockPos pos = new BlockPos(1, 64, 2);
        cache.setBlock(pos, BlockState.SOLID);
        assertEquals(BlockState.SOLID, cache.blockAt(pos));
    }

    @Test
    void isSolidReflectsState() {
        WorldCache cache = new WorldCache();
        BlockPos solid = new BlockPos(1, 1, 1);
        BlockPos passable = new BlockPos(2, 2, 2);
        cache.setBlock(solid, BlockState.SOLID);
        cache.setBlock(passable, BlockState.PASSABLE);
        assertTrue(cache.isSolid(solid));
        assertFalse(cache.isSolid(passable));
        assertFalse(cache.isSolid(new BlockPos(9, 9, 9)));
    }

    @Test
    void settingUnknownForgetsTheBlock() {
        WorldCache cache = new WorldCache();
        BlockPos pos = new BlockPos(3, 3, 3);
        cache.setBlock(pos, BlockState.SOLID);
        cache.setBlock(pos, BlockState.UNKNOWN);
        assertEquals(BlockState.UNKNOWN, cache.blockAt(pos));
        assertEquals(0, cache.size());
    }

    @Test
    void clearForgetsEveryBlock() {
        WorldCache cache = new WorldCache();
        cache.setBlock(new BlockPos(0, 0, 0), BlockState.SOLID);
        cache.setBlock(new BlockPos(1, 0, 0), BlockState.SOLID);
        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    void rejectsNullArguments() {
        WorldCache cache = new WorldCache();
        assertThrows(NullPointerException.class,
                () -> cache.setBlock(null, BlockState.SOLID));
        assertThrows(NullPointerException.class,
                () -> cache.setBlock(new BlockPos(0, 0, 0), null));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-world:test --tests "com.github.axiom.ac.world.WorldCacheTest"`
Expected: FAIL — compilation error, `WorldCache` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.world;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side cache of known block collision states. Uncached blocks
 * read back as {@link BlockState#UNKNOWN}. Populated by callers from
 * block updates and chunk data.
 *
 * <p>Thread-safe: block updates arrive on the server thread while
 * lookups happen on netty threads.
 */
public final class WorldCache {

    private final Map<BlockPos, BlockState> blocks = new ConcurrentHashMap<>();

    /**
     * Records the collision state of {@code pos}. Setting
     * {@link BlockState#UNKNOWN} forgets the block.
     */
    public void setBlock(BlockPos pos, BlockState state) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
        if (state == BlockState.UNKNOWN) {
            blocks.remove(pos);
        } else {
            blocks.put(pos, state);
        }
    }

    /** The cached state of {@code pos}, or {@code UNKNOWN} if absent. */
    public BlockState blockAt(BlockPos pos) {
        return blocks.getOrDefault(pos, BlockState.UNKNOWN);
    }

    /** True only when {@code pos} is cached and {@code SOLID}. */
    public boolean isSolid(BlockPos pos) {
        return blockAt(pos) == BlockState.SOLID;
    }

    /** Forgets every cached block. */
    public void clear() {
        blocks.clear();
    }

    /** Number of cached (non-{@code UNKNOWN}) blocks. */
    public int size() {
        return blocks.size();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-world:test --tests "com.github.axiom.ac.world.WorldCacheTest"`
Expected: PASS — 6 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-world/src/main/java/com/github/axiom/ac/world/WorldCache.java axiom-world/src/test/java/com/github/axiom/ac/world/WorldCacheTest.java
git commit -m "feat: add WorldCache block-state cache to axiom-world"
```

---

## Task 5: CollisionEngine — AABB-vs-world and voxel raycast

**Files:**
- Create: `axiom-world/src/main/java/com/github/axiom/ac/world/CollisionEngine.java`
- Test: `axiom-world/src/test/java/com/github/axiom/ac/world/CollisionEngineTest.java`

`collides` reports whether an `Aabb` overlaps any solid block: it visits every integer voxel the box spans (`floor(min)` to `ceil(max) - 1` per axis, matching `Aabb`'s strict-overlap rule) and returns true on the first solid one. `raycast` does Amanatides–Woo voxel traversal along a ray, returning the first solid block within `maxDistance` (the ray direction is normalised so distance is in world units).

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.Ray;
import com.github.axiom.ac.math.Vec3;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CollisionEngineTest {

    private final WorldCache world = new WorldCache();
    private final CollisionEngine engine = new CollisionEngine(world);

    @Test
    void collidesIsFalseInEmptyWorld() {
        assertFalse(engine.collides(new Aabb(0, 0, 0, 1, 1, 1)));
    }

    @Test
    void collidesDetectsOverlappingSolidBlock() {
        world.setBlock(new BlockPos(5, 64, 5), BlockState.SOLID);
        assertTrue(engine.collides(new Aabb(4.7, 64.0, 4.7, 5.3, 65.8, 5.3)));
    }

    @Test
    void collidesIgnoresPassableBlock() {
        world.setBlock(new BlockPos(5, 64, 5), BlockState.PASSABLE);
        assertFalse(engine.collides(new Aabb(5.1, 64.1, 5.1, 5.9, 64.9, 5.9)));
    }

    @Test
    void collidesExcludesBlockTouchedOnlyAtFace() {
        world.setBlock(new BlockPos(5, 64, 5), BlockState.SOLID);
        // Box sits exactly on top of the block: shared face, no overlap.
        assertFalse(engine.collides(new Aabb(5.0, 65.0, 5.0, 5.9, 66.0, 5.9)));
    }

    @Test
    void raycastHitsSolidBlockAhead() {
        world.setBlock(new BlockPos(10, 0, 0), BlockState.SOLID);
        Ray ray = new Ray(new Vec3(0.5, 0.5, 0.5), new Vec3(1, 0, 0));
        Optional<BlockPos> hit = engine.raycast(ray, 20.0);
        assertEquals(Optional.of(new BlockPos(10, 0, 0)), hit);
    }

    @Test
    void raycastMissesWhenNothingInRange() {
        world.setBlock(new BlockPos(100, 0, 0), BlockState.SOLID);
        Ray ray = new Ray(new Vec3(0.5, 0.5, 0.5), new Vec3(1, 0, 0));
        assertTrue(engine.raycast(ray, 20.0).isEmpty());
    }

    @Test
    void raycastReturnsBlockContainingOriginWhenSolid() {
        world.setBlock(new BlockPos(0, 0, 0), BlockState.SOLID);
        Ray ray = new Ray(new Vec3(0.5, 0.5, 0.5), new Vec3(1, 0, 0));
        assertEquals(Optional.of(new BlockPos(0, 0, 0)), engine.raycast(ray, 5.0));
    }

    @Test
    void raycastStopsAtFirstSolidBlock() {
        world.setBlock(new BlockPos(3, 0, 0), BlockState.SOLID);
        world.setBlock(new BlockPos(7, 0, 0), BlockState.SOLID);
        Ray ray = new Ray(new Vec3(0.5, 0.5, 0.5), new Vec3(1, 0, 0));
        assertEquals(Optional.of(new BlockPos(3, 0, 0)), engine.raycast(ray, 20.0));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-world:test --tests "com.github.axiom.ac.world.CollisionEngineTest"`
Expected: FAIL — compilation error, `CollisionEngine` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.world;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.Ray;
import com.github.axiom.ac.math.Vec3;
import java.util.Objects;
import java.util.Optional;

/**
 * Queries collisions between geometry and the cached world. Every
 * solid block is treated as a full unit cube.
 */
public final class CollisionEngine {

    private final WorldCache world;

    public CollisionEngine(WorldCache world) {
        this.world = Objects.requireNonNull(world, "world");
    }

    /**
     * True when {@code box} overlaps any solid block. A block touched
     * only at a shared face does not count, matching {@link Aabb}'s
     * strict-overlap rule.
     */
    public boolean collides(Aabb box) {
        int minX = (int) Math.floor(box.minX());
        int minY = (int) Math.floor(box.minY());
        int minZ = (int) Math.floor(box.minZ());
        int maxX = (int) Math.ceil(box.maxX()) - 1;
        int maxY = (int) Math.ceil(box.maxY()) - 1;
        int maxZ = (int) Math.ceil(box.maxZ()) - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.isSolid(new BlockPos(x, y, z))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Traverses {@code ray} through the voxel grid and returns the
     * first solid block within {@code maxDistance} world units, or
     * empty when none is hit. Uses Amanatides–Woo voxel traversal.
     */
    public Optional<BlockPos> raycast(Ray ray, double maxDistance) {
        Vec3 direction = ray.direction();
        double length = direction.length();
        if (length == 0.0) {
            return Optional.empty();
        }
        Vec3 dir = direction.scale(1.0 / length);
        Vec3 origin = ray.origin();

        int x = (int) Math.floor(origin.x());
        int y = (int) Math.floor(origin.y());
        int z = (int) Math.floor(origin.z());

        int stepX = signum(dir.x());
        int stepY = signum(dir.y());
        int stepZ = signum(dir.z());

        double tMaxX = boundaryDistance(origin.x(), dir.x(), stepX);
        double tMaxY = boundaryDistance(origin.y(), dir.y(), stepY);
        double tMaxZ = boundaryDistance(origin.z(), dir.z(), stepZ);

        double tDeltaX = dir.x() == 0.0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.x());
        double tDeltaY = dir.y() == 0.0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.y());
        double tDeltaZ = dir.z() == 0.0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.z());

        double travelled = 0.0;
        while (travelled <= maxDistance) {
            if (world.isSolid(new BlockPos(x, y, z))) {
                return Optional.of(new BlockPos(x, y, z));
            }
            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                travelled = tMaxX;
                tMaxX += tDeltaX;
                x += stepX;
            } else if (tMaxY <= tMaxZ) {
                travelled = tMaxY;
                tMaxY += tDeltaY;
                y += stepY;
            } else {
                travelled = tMaxZ;
                tMaxZ += tDeltaZ;
                z += stepZ;
            }
        }
        return Optional.empty();
    }

    private static int signum(double value) {
        if (value > 0.0) {
            return 1;
        }
        if (value < 0.0) {
            return -1;
        }
        return 0;
    }

    /**
     * Distance along the ray from {@code coordinate} to the first
     * voxel boundary in the stepping direction.
     */
    private static double boundaryDistance(double coordinate, double dir, int step) {
        if (step == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double current = Math.floor(coordinate);
        double next = step > 0 ? current + 1.0 : current;
        return (next - coordinate) / dir;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-world:test --tests "com.github.axiom.ac.world.CollisionEngineTest"`
Expected: PASS — 8 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-world/src/main/java/com/github/axiom/ac/world/CollisionEngine.java axiom-world/src/test/java/com/github/axiom/ac/world/CollisionEngineTest.java
git commit -m "feat: add CollisionEngine to axiom-world"
```

---

## Task 6: PhysicsSimulator — collision-aware tick prediction

**Files:**
- Create: `axiom-world/src/main/java/com/github/axiom/ac/world/PhysicsSimulator.java`
- Test: `axiom-world/src/test/java/com/github/axiom/ac/world/PhysicsSimulatorTest.java`

Predicts where a player should be after one tick. It applies the `axiom-math` `MotionFormulas` (gravity to vertical velocity, ground friction to horizontal velocity), then resolves the resulting motion against solid blocks axis by axis (Y, then X, then Z) — a component that would drive the player's bounding box into a solid block is cancelled. The result is the new bounding box, the resolved velocity, and whether the player ended up supported from below.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.Vec3;
import org.junit.jupiter.api.Test;

class PhysicsSimulatorTest {

    private static final double EPS = 1e-9;

    private final WorldCache world = new WorldCache();
    private final CollisionEngine engine = new CollisionEngine(world);
    private final PhysicsSimulator simulator = new PhysicsSimulator(engine);

    /** Player box 0.6 wide, 1.8 tall, resting with its base at y. */
    private static Aabb playerBox(double x, double y, double z) {
        return new Aabb(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3);
    }

    @Test
    void fallsUnderGravityInEmptyWorld() {
        Aabb box = playerBox(0.5, 100.0, 0.5);
        PhysicsSimulator.Result result = simulator.simulate(box, new Vec3(0, 0, 0), false);
        // Vertical velocity from rest: (0 - 0.08) * 0.98 = -0.0784.
        assertEquals(-0.0784, result.velocity().y(), EPS);
        assertTrue(result.box().minY() < 100.0);
        assertFalse(result.onGround());
    }

    @Test
    void solidFloorStopsTheFallAndSupportsThePlayer() {
        // Floor of solid blocks at y = 63 (cube spans 63..64).
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlock(new BlockPos(x, 63, z), BlockState.SOLID);
            }
        }
        // Player resting exactly on top of the floor, at y = 64.
        Aabb box = playerBox(0.5, 64.0, 0.5);
        PhysicsSimulator.Result result = simulator.simulate(box, new Vec3(0, 0, 0), true);

        assertEquals(64.0, result.box().minY(), EPS);
        assertEquals(0.0, result.velocity().y(), EPS);
        assertTrue(result.onGround());
    }

    @Test
    void horizontalVelocityDecaysWithFriction() {
        Aabb box = playerBox(0.5, 100.0, 0.5);
        PhysicsSimulator.Result result = simulator.simulate(box, new Vec3(1.0, 0, 0), false);
        // Horizontal friction: 0.6 * 0.91 = 0.546.
        assertEquals(0.546, result.velocity().x(), EPS);
    }

    @Test
    void solidWallCancelsHorizontalMotion() {
        // Wall of solid blocks at x = 1 around the player's height.
        for (int y = 99; y <= 102; y++) {
            world.setBlock(new BlockPos(1, y, 0), BlockState.SOLID);
        }
        // Player just west of the wall, moving east into it.
        Aabb box = playerBox(0.7, 100.0, 0.5);
        PhysicsSimulator.Result result = simulator.simulate(box, new Vec3(1.0, 0, 0), false);

        // Eastward motion is cancelled; the box does not enter x >= 1.
        assertTrue(result.box().maxX() <= 1.0 + EPS);
        assertEquals(0.0, result.velocity().x(), EPS);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-world:test --tests "com.github.axiom.ac.world.PhysicsSimulatorTest"`
Expected: FAIL — compilation error, `PhysicsSimulator` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.world;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.MotionFormulas;
import com.github.axiom.ac.math.Vec3;
import java.util.Objects;

/**
 * Predicts a player's motion over one tick, resolving collisions
 * against the cached world.
 *
 * <p>Vertical velocity decays under gravity and drag; horizontal
 * velocity decays under ground friction. The resulting motion is
 * then resolved axis by axis (Y, X, Z): a component that would push
 * the player's bounding box into a solid block is cancelled.
 */
public final class PhysicsSimulator {

    private final CollisionEngine collision;

    public PhysicsSimulator(CollisionEngine collision) {
        this.collision = Objects.requireNonNull(collision, "collision");
    }

    /**
     * Result of one simulated tick.
     *
     * @param box      the player bounding box after the tick
     * @param velocity the velocity after collision resolution
     * @param onGround whether the player ended supported from below
     */
    public record Result(Aabb box, Vec3 velocity, boolean onGround) {
    }

    /**
     * Simulates one tick for a player whose bounding box is
     * {@code box}, current velocity is {@code velocity}, and
     * client-reported ground state is {@code onGround}.
     */
    public Result simulate(Aabb box, Vec3 velocity, boolean onGround) {
        double friction = MotionFormulas.horizontalFriction(MotionFormulas.DEFAULT_SLIPPERINESS);
        double wantX = MotionFormulas.nextHorizontalVelocity(velocity.x(), friction);
        double wantY = MotionFormulas.nextVerticalVelocity(velocity.y());
        double wantZ = MotionFormulas.nextHorizontalVelocity(velocity.z(), friction);

        double resolvedY = resolveAxis(box, 0.0, wantY, 0.0);
        Aabb afterY = box.offset(0.0, resolvedY, 0.0);

        double resolvedX = resolveAxis(afterY, wantX, 0.0, 0.0);
        Aabb afterX = afterY.offset(resolvedX, 0.0, 0.0);

        double resolvedZ = resolveAxis(afterX, 0.0, 0.0, wantZ);
        Aabb afterZ = afterX.offset(0.0, 0.0, resolvedZ);

        boolean supported = wantY <= 0.0 && resolvedY == 0.0;
        Vec3 resolvedVelocity = new Vec3(resolvedX, resolvedY, resolvedZ);
        return new Result(afterZ, resolvedVelocity, supported);
    }

    /**
     * Returns the requested single-axis movement, or 0 when applying
     * it would drive {@code box} into a solid block.
     */
    private double resolveAxis(Aabb box, double dx, double dy, double dz) {
        double requested = dx + dy + dz;
        if (requested == 0.0) {
            return 0.0;
        }
        return collision.collides(box.offset(dx, dy, dz)) ? 0.0 : requested;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-world:test --tests "com.github.axiom.ac.world.PhysicsSimulatorTest"`
Expected: PASS — 4 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-world/src/main/java/com/github/axiom/ac/world/PhysicsSimulator.java axiom-world/src/test/java/com/github/axiom/ac/world/PhysicsSimulatorTest.java
git commit -m "feat: add PhysicsSimulator collision-aware prediction to axiom-world"
```

---

## Task 7: Full module verification

**Files:** none — verification only.

- [ ] **Step 1: Run the module test suite**

Run: `.\gradlew.bat :axiom-world:test`
Expected: PASS — `BUILD SUCCESSFUL`, 4 test classes green (22 tests total).

- [ ] **Step 2: Build the whole project**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL` — `axiom-math`, `axiom-api`, `axiom-packet`, `axiom-world` all build and test green.

- [ ] **Step 3: Commit if anything changed**

If steps 1-2 produced no source changes, skip. Otherwise commit with message `build: verify axiom-world module builds and tests pass`.

---

## Plan Complete

`axiom-world` provides the server-side world model: `BlockPos`/`BlockState` (block identity and collision state), `WorldCache` (the block-state cache), `CollisionEngine` (AABB-vs-world and voxel raycasting), and `PhysicsSimulator` (collision-aware single-tick prediction).

**Next:** Plan 5 — `axiom-core` (hybrid runtime, check registry, storage providers) and `axiom-plugin` (the Paper plugin bootstrap). Requires its own plan.
