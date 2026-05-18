# Axiom AC — Predict Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `axiom-predict`, the deterministic movement-prediction engine: it enumerates the inputs a client could legitimately have pressed, simulates each through the collision-aware physics, and reports the prediction closest to where the client actually claims to be — the offset between them is the cheat signal.

**Architecture:** A Gradle module depending only on `axiom-math` and `axiom-world`. It is pure, deterministic and fully unit-tested. It builds on `axiom-world`'s `PhysicsSimulator`: the prediction engine adds input-driven acceleration to a player's velocity, then delegates the collision-aware tick to the simulator.

**Tech Stack:** Java 21, Gradle (multi-module), JUnit 6.

This plan is **Plan 6** for the Axiom AC toolkit — the Phase 2 module reserved in `docs/superpowers/specs/2026-05-18-axiom-ac-design.md` (sections 4 and 10). Plans 1-5 (all six Phase 1 modules) are merged.

**Spec refinement:** the design spec's dependency graph listed `predict → core+world`. This plan makes `axiom-predict` depend only on `axiom-math` and `axiom-world`. Rationale: prediction is pure physics math over geometry — it needs the `PhysicsSimulator` and `Vec3`/`Aabb`, not the runtime. Consumers feed player state in and read the offset out; wiring prediction into a check is the consumer's job.

**Physics-accuracy scope (IMPORTANT):** the input-acceleration model in this module is a **documented baseline approximation**. It uses Minecraft's `moveRelative` direction math and plausible movement constants, but does not reproduce every per-version nuance (sprint-jump boost, slipperiness-dependent ground acceleration, sneaking, fluids, status effects). The *engine* — input enumeration, simulation, offset search — is exact and testable; tuning the constants to a specific Minecraft version so that the offset is near-zero for legitimate play is explicit future work. Checks built on this should treat the offset as a relative signal, not an absolute, until the model is tuned.

---

## File Structure

```
axiom-libraryac/
├── settings.gradle                          MODIFY — add axiom-predict module
├── axiom-predict/
│   ├── build.gradle                         CREATE — depends on axiom-math, axiom-world
│   └── src/
│       ├── main/java/com/github/axiom/ac/predict/
│       │   ├── MovementInput.java            One candidate set of client inputs
│       │   ├── InputSpace.java               Enumeration of all candidate inputs
│       │   ├── PlayerState.java              Position/velocity/yaw/ground snapshot
│       │   ├── PredictionEngine.java         Single-tick input-driven prediction
│       │   ├── PredictionResult.java         A prediction plus its offset
│       │   └── MovementPredictor.java        Best-match search over the input space
│       └── test/java/com/github/axiom/ac/predict/
│           └── (test classes per the tasks below)
```

**Windows note:** commands use `.\gradlew.bat` (PowerShell).

---

## Task 1: Add the axiom-predict module

**Files:**
- Modify: `settings.gradle`
- Create: `axiom-predict/build.gradle`

- [ ] **Step 1: Replace `settings.gradle`**

```groovy
rootProject.name = 'axiom-libraryac'

include 'axiom-math'
include 'axiom-api'
include 'axiom-packet'
include 'axiom-world'
include 'axiom-core'
include 'axiom-plugin'
include 'axiom-predict'
```

- [ ] **Step 2: Create `axiom-predict/build.gradle`**

```groovy
// axiom-predict: deterministic movement-prediction engine. Pure
// physics math; depends only on axiom-math and axiom-world. Build
// configuration is inherited from the root.
dependencies {
    implementation project(':axiom-math')
    implementation project(':axiom-world')
}
```

- [ ] **Step 3: Verify the module is recognised**

Run: `.\gradlew.bat projects`
Expected: output lists `Project ':axiom-predict'`.

- [ ] **Step 4: Commit**

```bash
git add settings.gradle axiom-predict/build.gradle
git commit -m "build: add axiom-predict module"
```

---

## Task 2: MovementInput — one candidate set of client inputs

**Files:**
- Create: `axiom-predict/src/main/java/com/github/axiom/ac/predict/MovementInput.java`
- Test: `axiom-predict/src/test/java/com/github/axiom/ac/predict/MovementInputTest.java`

The inputs a client may have pressed for one tick: a forward axis and a strafe axis (each `-1`, `0`, or `+1`), plus jump and sprint flags. The constructor rejects axis values outside `{-1, 0, 1}`.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MovementInputTest {

    @Test
    void exposesItsFields() {
        MovementInput input = new MovementInput(1, -1, true, false);
        assertEquals(1, input.forward());
        assertEquals(-1, input.strafe());
        assertEquals(true, input.jump());
        assertEquals(false, input.sprint());
    }

    @Test
    void noneIsAllZeroAndUnpressed() {
        MovementInput none = MovementInput.none();
        assertEquals(0, none.forward());
        assertEquals(0, none.strafe());
        assertFalse(none.jump());
        assertFalse(none.sprint());
    }

    @Test
    void rejectsForwardOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new MovementInput(2, 0, false, false));
    }

    @Test
    void rejectsStrafeOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new MovementInput(0, -2, false, false));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-predict:test --tests "com.github.axiom.ac.predict.MovementInputTest"`
Expected: FAIL — compilation error, `MovementInput` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.predict;

/**
 * One candidate set of client movement inputs for a single tick.
 *
 * @param forward forward axis: {@code -1} (back), {@code 0}, {@code +1}
 * @param strafe  strafe axis: {@code -1}, {@code 0}, {@code +1}
 * @param jump    whether the jump key was held
 * @param sprint  whether the sprint key was held
 */
public record MovementInput(int forward, int strafe, boolean jump, boolean sprint) {

    public MovementInput {
        requireAxis(forward, "forward");
        requireAxis(strafe, "strafe");
    }

    private static void requireAxis(int value, String name) {
        if (value < -1 || value > 1) {
            throw new IllegalArgumentException(name + " must be -1, 0 or 1");
        }
    }

    /** The neutral input: no movement, no jump, no sprint. */
    public static MovementInput none() {
        return new MovementInput(0, 0, false, false);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-predict:test --tests "com.github.axiom.ac.predict.MovementInputTest"`
Expected: PASS — 4 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-predict/src/main/java/com/github/axiom/ac/predict/MovementInput.java axiom-predict/src/test/java/com/github/axiom/ac/predict/MovementInputTest.java
git commit -m "feat: add MovementInput to axiom-predict"
```

---

## Task 3: InputSpace — enumeration of all candidate inputs

**Files:**
- Create: `axiom-predict/src/main/java/com/github/axiom/ac/predict/InputSpace.java`
- Test: `axiom-predict/src/test/java/com/github/axiom/ac/predict/InputSpaceTest.java`

Enumerates every `MovementInput` a client could have pressed: 3 forward values × 3 strafe values × 2 jump × 2 sprint = 36 combinations. The prediction search tries all of them.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InputSpaceTest {

    @Test
    void enumeratesEveryCombination() {
        // 3 forward * 3 strafe * 2 jump * 2 sprint.
        assertEquals(36, InputSpace.all().size());
    }

    @Test
    void everyInputIsDistinct() {
        List<MovementInput> all = InputSpace.all();
        assertEquals(all.size(), Set.copyOf(all).size());
    }

    @Test
    void includesTheNeutralInput() {
        assertTrue(InputSpace.all().contains(MovementInput.none()));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-predict:test --tests "com.github.axiom.ac.predict.InputSpaceTest"`
Expected: FAIL — compilation error, `InputSpace` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.predict;

import java.util.ArrayList;
import java.util.List;

/**
 * The full set of {@link MovementInput}s a client could have pressed
 * in one tick. The prediction search evaluates every entry.
 */
public final class InputSpace {

    private static final int[] AXES = {-1, 0, 1};
    private static final boolean[] FLAGS = {false, true};

    private static final List<MovementInput> ALL = enumerate();

    private InputSpace() {
    }

    private static List<MovementInput> enumerate() {
        List<MovementInput> inputs = new ArrayList<>();
        for (int forward : AXES) {
            for (int strafe : AXES) {
                for (boolean jump : FLAGS) {
                    for (boolean sprint : FLAGS) {
                        inputs.add(new MovementInput(forward, strafe, jump, sprint));
                    }
                }
            }
        }
        return List.copyOf(inputs);
    }

    /** An immutable list of every candidate input. */
    public static List<MovementInput> all() {
        return ALL;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-predict:test --tests "com.github.axiom.ac.predict.InputSpaceTest"`
Expected: PASS — 3 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-predict/src/main/java/com/github/axiom/ac/predict/InputSpace.java axiom-predict/src/test/java/com/github/axiom/ac/predict/InputSpaceTest.java
git commit -m "feat: add InputSpace enumeration to axiom-predict"
```

---

## Task 4: PlayerState — a movement snapshot

**Files:**
- Create: `axiom-predict/src/main/java/com/github/axiom/ac/predict/PlayerState.java`
- Test: `axiom-predict/src/test/java/com/github/axiom/ac/predict/PlayerStateTest.java`

An immutable snapshot of the inputs and outputs of one prediction step: feet position, velocity, look yaw, and ground state. Geometry types come from `axiom-math`.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import org.junit.jupiter.api.Test;

class PlayerStateTest {

    @Test
    void exposesItsFields() {
        PlayerState state = new PlayerState(
                new Vec3(1, 64, 2), new Vec3(0.1, 0, 0.2), 90.0f, true);
        assertEquals(new Vec3(1, 64, 2), state.position());
        assertEquals(new Vec3(0.1, 0, 0.2), state.velocity());
        assertEquals(90.0f, state.yaw());
        assertTrue(state.onGround());
    }

    @Test
    void rejectsNullPosition() {
        assertThrows(NullPointerException.class,
                () -> new PlayerState(null, new Vec3(0, 0, 0), 0.0f, false));
    }

    @Test
    void rejectsNullVelocity() {
        assertThrows(NullPointerException.class,
                () -> new PlayerState(new Vec3(0, 0, 0), null, 0.0f, false));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-predict:test --tests "com.github.axiom.ac.predict.PlayerStateTest"`
Expected: FAIL — compilation error, `PlayerState` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.predict;

import com.github.axiom.ac.math.Vec3;
import java.util.Objects;

/**
 * Immutable snapshot of a player's movement state — the input and
 * output type of a prediction step.
 *
 * @param position feet position
 * @param velocity velocity (movement delta over the last tick)
 * @param yaw      horizontal look angle, in degrees
 * @param onGround whether the player is supported from below
 */
public record PlayerState(Vec3 position, Vec3 velocity, float yaw, boolean onGround) {

    public PlayerState {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(velocity, "velocity");
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-predict:test --tests "com.github.axiom.ac.predict.PlayerStateTest"`
Expected: PASS — 3 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-predict/src/main/java/com/github/axiom/ac/predict/PlayerState.java axiom-predict/src/test/java/com/github/axiom/ac/predict/PlayerStateTest.java
git commit -m "feat: add PlayerState snapshot to axiom-predict"
```

---

## Task 5: PredictionEngine — single-tick input-driven prediction

**Files:**
- Create: `axiom-predict/src/main/java/com/github/axiom/ac/predict/PredictionEngine.java`
- Test: `axiom-predict/src/test/java/com/github/axiom/ac/predict/PredictionEngineTest.java`

Given a previous `PlayerState` and a candidate `MovementInput`, predicts the next `PlayerState`. It converts the input axes into a world-space acceleration using Minecraft's `moveRelative` direction math (the input vector is normalised, scaled by a move speed, and rotated by the look yaw), adds it to the velocity, applies a jump impulse when jumping from the ground, then delegates the collision-aware tick to `axiom-world`'s `PhysicsSimulator`. The standard player bounding box (0.6 wide, 1.8 tall) is built around the feet position for the physics step.

See the "Physics-accuracy scope" note at the top of this plan — the movement constants are a baseline approximation.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import com.github.axiom.ac.world.CollisionEngine;
import com.github.axiom.ac.world.PhysicsSimulator;
import com.github.axiom.ac.world.WorldCache;
import org.junit.jupiter.api.Test;

class PredictionEngineTest {

    /** Engine over an empty world (no blocks, nothing collides). */
    private static PredictionEngine engineInEmptyWorld() {
        PhysicsSimulator simulator = new PhysicsSimulator(new CollisionEngine(new WorldCache()));
        return new PredictionEngine(simulator);
    }

    @Test
    void rejectsNullSimulator() {
        assertThrows(NullPointerException.class, () -> new PredictionEngine(null));
    }

    @Test
    void neutralInputJustFallsUnderGravity() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, false);

        PlayerState next = engine.predict(start, MovementInput.none());

        // No horizontal input -> no horizontal motion.
        assertEquals(0.0, next.position().x(), 1e-9);
        assertEquals(0.0, next.position().z(), 1e-9);
        // Gravity pulls the player down.
        assertTrue(next.position().y() < 100.0);
    }

    @Test
    void forwardInputAtZeroYawMovesAlongPositiveZ() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, false);

        PlayerState next = engine.predict(start, new MovementInput(1, 0, false, false));

        // At yaw 0, "forward" is +Z in Minecraft's convention.
        assertTrue(next.position().z() > 0.0);
        assertEquals(0.0, next.position().x(), 1e-9);
    }

    @Test
    void sprintForwardOutrunsWalkForward() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, false);

        double walk = engine.predict(start,
                new MovementInput(1, 0, false, false)).position().z();
        double sprint = engine.predict(start,
                new MovementInput(1, 0, false, true)).position().z();

        assertTrue(sprint > walk);
    }

    @Test
    void jumpFromGroundProducesUpwardMotion() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, true);

        PlayerState next = engine.predict(start, new MovementInput(0, 0, true, false));

        // A jump impulse beats one tick of gravity: the player rises.
        assertTrue(next.position().y() > 100.0);
    }

    @Test
    void jumpInTheAirDoesNotRise() {
        PredictionEngine engine = engineInEmptyWorld();
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, false);

        PlayerState next = engine.predict(start, new MovementInput(0, 0, true, false));

        // Not on the ground: the jump key does nothing, gravity wins.
        assertTrue(next.position().y() < 100.0);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-predict:test --tests "com.github.axiom.ac.predict.PredictionEngineTest"`
Expected: FAIL — compilation error, `PredictionEngine` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.predict;

import com.github.axiom.ac.math.Aabb;
import com.github.axiom.ac.math.MotionFormulas;
import com.github.axiom.ac.math.Vec3;
import com.github.axiom.ac.world.PhysicsSimulator;
import java.util.Objects;

/**
 * Predicts the next {@link PlayerState} for a candidate
 * {@link MovementInput}. Input axes become a world-space acceleration
 * via Minecraft's {@code moveRelative} direction math; the result is
 * added to the velocity, a jump impulse is applied when jumping from
 * the ground, and the collision-aware tick is delegated to
 * {@link PhysicsSimulator}.
 *
 * <p>The movement constants are a baseline approximation — see the
 * plan's "Physics-accuracy scope" note. The engine is exact; the
 * constants are not yet tuned to a specific Minecraft version.
 */
public final class PredictionEngine {

    /** Width of the standard player bounding box. */
    public static final double PLAYER_WIDTH = 0.6;

    /** Height of the standard player bounding box. */
    public static final double PLAYER_HEIGHT = 1.8;

    /** Baseline per-tick horizontal acceleration for walking. */
    public static final double BASE_MOVE_SPEED = 0.1;

    /** Multiplier applied to the move speed while sprinting. */
    public static final double SPRINT_MULTIPLIER = 1.3;

    private static final double INPUT_EPSILON = 1.0e-4;

    private final PhysicsSimulator simulator;

    public PredictionEngine(PhysicsSimulator simulator) {
        this.simulator = Objects.requireNonNull(simulator, "simulator");
    }

    /**
     * Predicts the state one tick after {@code previous}, assuming
     * the client pressed {@code input}.
     */
    public PlayerState predict(PlayerState previous, MovementInput input) {
        Vec3 acceleration = inputAcceleration(previous.yaw(), input);

        Vec3 velocity = previous.velocity();
        double velocityY = velocity.y();
        if (input.jump() && previous.onGround()) {
            velocityY = MotionFormulas.jumpVelocity(0);
        }
        Vec3 inputVelocity = new Vec3(
                velocity.x() + acceleration.x(),
                velocityY,
                velocity.z() + acceleration.z());

        Aabb box = boxAt(previous.position());
        PhysicsSimulator.Result result =
                simulator.simulate(box, inputVelocity, previous.onGround());
        return new PlayerState(feetOf(result.box()), result.velocity(),
                previous.yaw(), result.onGround());
    }

    /**
     * World-space horizontal acceleration produced by {@code input}
     * for a player looking along {@code yaw}. Uses Minecraft's
     * {@code moveRelative}: the input vector is normalised, scaled by
     * the move speed, and rotated by the yaw.
     */
    private static Vec3 inputAcceleration(float yaw, MovementInput input) {
        double strafe = input.strafe();
        double forward = input.forward();
        double magnitude = Math.sqrt(strafe * strafe + forward * forward);
        if (magnitude < INPUT_EPSILON) {
            return new Vec3(0, 0, 0);
        }
        double speed = BASE_MOVE_SPEED * (input.sprint() ? SPRINT_MULTIPLIER : 1.0);
        double scale = speed / Math.max(magnitude, 1.0);
        strafe *= scale;
        forward *= scale;

        double yawRad = Math.toRadians(yaw);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double accelX = strafe * cos - forward * sin;
        double accelZ = forward * cos + strafe * sin;
        return new Vec3(accelX, 0.0, accelZ);
    }

    /** The standard player bounding box around a feet position. */
    static Aabb boxAt(Vec3 feet) {
        double half = PLAYER_WIDTH / 2.0;
        return new Aabb(
                feet.x() - half, feet.y(), feet.z() - half,
                feet.x() + half, feet.y() + PLAYER_HEIGHT, feet.z() + half);
    }

    /** The feet position (bottom centre) of a player bounding box. */
    static Vec3 feetOf(Aabb box) {
        return new Vec3(
                (box.minX() + box.maxX()) / 2.0,
                box.minY(),
                (box.minZ() + box.maxZ()) / 2.0);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-predict:test --tests "com.github.axiom.ac.predict.PredictionEngineTest"`
Expected: PASS — 6 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-predict/src/main/java/com/github/axiom/ac/predict/PredictionEngine.java axiom-predict/src/test/java/com/github/axiom/ac/predict/PredictionEngineTest.java
git commit -m "feat: add PredictionEngine single-tick prediction to axiom-predict"
```

---

## Task 6: PredictionResult and MovementPredictor — best-match search

**Files:**
- Create: `axiom-predict/src/main/java/com/github/axiom/ac/predict/PredictionResult.java`
- Create: `axiom-predict/src/main/java/com/github/axiom/ac/predict/MovementPredictor.java`
- Test: `axiom-predict/src/test/java/com/github/axiom/ac/predict/MovementPredictorTest.java`

`MovementPredictor` runs the prediction engine over every input in the `InputSpace`, measures how far each prediction lands from where the client actually claims to be, and returns the closest one as a `PredictionResult`. The `offset` it carries — the distance between the best legitimate prediction and the actual position — is the cheat signal: near zero for honest movement, large when no legitimate input explains the client's move.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import com.github.axiom.ac.world.CollisionEngine;
import com.github.axiom.ac.world.PhysicsSimulator;
import com.github.axiom.ac.world.WorldCache;
import org.junit.jupiter.api.Test;

class MovementPredictorTest {

    private static PredictionEngine engineInEmptyWorld() {
        PhysicsSimulator simulator = new PhysicsSimulator(new CollisionEngine(new WorldCache()));
        return new PredictionEngine(simulator);
    }

    private final PredictionEngine engine = engineInEmptyWorld();
    private final MovementPredictor predictor = new MovementPredictor(engine);

    @Test
    void rejectsNullEngine() {
        assertThrows(NullPointerException.class, () -> new MovementPredictor(null));
    }

    @Test
    void aLegitimateMoveHasANearZeroOffset() {
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, true);
        // Where the player legitimately lands after sprinting forward.
        Vec3 honest = engine.predict(start,
                new MovementInput(1, 0, false, true)).position();

        PredictionResult result = predictor.bestPrediction(start, honest);

        assertEquals(0.0, result.offset(), 1e-6);
    }

    @Test
    void anImpossibleMoveHasALargeOffset() {
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, true);
        // A teleport far beyond anything one tick of input can produce.
        Vec3 cheat = new Vec3(50, 100, 0);

        PredictionResult result = predictor.bestPrediction(start, cheat);

        assertTrue(result.offset() > 10.0);
    }

    @Test
    void resultCarriesTheBestInputAndPrediction() {
        PlayerState start = new PlayerState(
                new Vec3(0, 100, 0), new Vec3(0, 0, 0), 0.0f, true);
        Vec3 honest = engine.predict(start,
                new MovementInput(1, 0, false, true)).position();

        PredictionResult result = predictor.bestPrediction(start, honest);

        // The chosen prediction is the one nearest the actual position.
        assertEquals(result.offset(),
                result.predicted().position().distance(honest), 1e-9);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-predict:test --tests "com.github.axiom.ac.predict.MovementPredictorTest"`
Expected: FAIL — compilation error, `MovementPredictor` and `PredictionResult` do not exist.

- [ ] **Step 3: Write the implementations**

Create `PredictionResult.java`:

```java
package com.github.axiom.ac.predict;

/**
 * The outcome of a best-match prediction search.
 *
 * @param input     the candidate input whose prediction matched best
 * @param predicted the predicted player state for that input
 * @param offset    distance between the predicted position and the
 *                  client's actual position — the cheat signal
 */
public record PredictionResult(MovementInput input, PlayerState predicted,
                               double offset) {
}
```

Create `MovementPredictor.java`:

```java
package com.github.axiom.ac.predict;

import com.github.axiom.ac.math.Vec3;
import java.util.Objects;

/**
 * Searches the whole {@link InputSpace} for the input whose predicted
 * outcome lands closest to where the client actually claims to be.
 * The resulting {@link PredictionResult#offset()} is the movement
 * cheat signal: near zero for legitimate play, large when no
 * legitimate input explains the move.
 */
public final class MovementPredictor {

    private final PredictionEngine engine;

    public MovementPredictor(PredictionEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    /**
     * Returns the best-matching prediction for a player moving from
     * {@code previous} to {@code actualPosition}.
     */
    public PredictionResult bestPrediction(PlayerState previous, Vec3 actualPosition) {
        PredictionResult best = null;
        for (MovementInput input : InputSpace.all()) {
            PlayerState predicted = engine.predict(previous, input);
            double offset = predicted.position().distance(actualPosition);
            if (best == null || offset < best.offset()) {
                best = new PredictionResult(input, predicted, offset);
            }
        }
        return best;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-predict:test --tests "com.github.axiom.ac.predict.MovementPredictorTest"`
Expected: PASS — 4 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-predict/src/main/java/com/github/axiom/ac/predict/PredictionResult.java axiom-predict/src/main/java/com/github/axiom/ac/predict/MovementPredictor.java axiom-predict/src/test/java/com/github/axiom/ac/predict/MovementPredictorTest.java
git commit -m "feat: add MovementPredictor best-match search to axiom-predict"
```

---

## Task 7: Full project verification

**Files:** none — verification only.

- [ ] **Step 1: Run the module test suite**

Run: `.\gradlew.bat :axiom-predict:test`
Expected: PASS — `BUILD SUCCESSFUL`, 5 test classes green (20 tests total).

- [ ] **Step 2: Build the whole project**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL` — all seven modules build and test green.

- [ ] **Step 3: Commit if anything changed**

If steps 1-2 produced no source changes, skip. Otherwise commit with message `build: verify axiom-predict module builds and tests pass`.

---

## Plan Complete

`axiom-predict` provides the movement-prediction engine: `MovementInput` and `InputSpace` (the candidate inputs), `PlayerState` (the movement snapshot), `PredictionEngine` (single-tick input-driven prediction over `axiom-world`'s physics), and `PredictionResult`/`MovementPredictor` (the best-match search that produces the offset cheat signal).

This completes the Axiom AC toolkit including the Phase 2 prediction engine — all seven modules of the design are implemented.

**Future work:** tune the `PredictionEngine` movement constants to a specific Minecraft version so the offset is near-zero for legitimate play across all movement situations (sprint-jump, slipperiness, sneaking, fluids, status effects). The engine and search are exact; only the physics constants need version-specific calibration.
