# Axiom Detect

`axiom-detect` is the check-building toolkit. Where [Writing a Check](Writing-a-Check.md)
shows the bare `Check` SPI, this module gives you the parts most checks are
made of: per-player state, confidence mapping, heuristic and statistical check
bases, a raytrace engine, and a prediction-offset probe. Like the rest of
Axiom it **ships no concrete checks** — it is the scaffolding you extend.

One subpackage per concern:

| Subpackage | Gives you |
|------------|-----------|
| `detect.session` | `SessionStore<S>` — per-player state for an otherwise stateless check |
| `detect.signal` | `Confidence` — map a raw magnitude onto the `[0, 1]` a `Violation` needs |
| `detect.heuristic` | `AbstractHeuristicCheck` + `ViolationLevel`, `HeuristicSignal` |
| `detect.statistical` | `AbstractStatisticalCheck` + `StatisticalCriterion` and four implementations |
| `detect.raytrace` | `RaytraceEngine`, `ReachResolver`, `LineOfSight`, `LookVectors`, `Hitbox`, `RayHit` |
| `detect.prediction` | `PredictionProbe` — drive `axiom-predict` from a live state stream |

## Depending on it

`axiom-detect` builds on `axiom-api`, `axiom-math`, `axiom-world` and
`axiom-predict`. With a composite build (see [Getting Started](Getting-Started.md)):

```groovy
dependencies {
    implementation 'com.github.axiom.ac:axiom-detect'
    implementation 'com.github.axiom.ac:axiom-api'   // Check, Violation, PlayerData
}
```

## Which tool for which check

| You are detecting… | Use | Why |
|--------------------|-----|-----|
| A pattern that builds over time (speed, fly, phase) | `AbstractHeuristicCheck` | One bad tick is noise; a rising violation level is signal |
| A value that drifts from the player's own baseline | `AbstractStatisticalCheck` + `ZScoreCriterion` / `IqrCriterion` | Adaptive, per-player thresholds beat fixed constants |
| Machine-like regularity (autoclicker, timer) | `RegularityCriterion` / `PeriodicityCriterion` | Human input is noisy; bots are not |
| Reach, hitbox, line of sight (killaura, reach, hitbox) | `RaytraceEngine` / `ReachResolver` / `LineOfSight` | Pure geometry against the cached world |
| Movement that no legitimate input explains | `PredictionProbe` | Turns the prediction offset into a smoothed signal |

---

## Foundation: state and confidence

A `Check` is stateless by contract, so any memory it needs lives in a
`SessionStore<S>` — a concurrent, UUID-keyed map. The check bases below each
hold one internally, so you rarely touch it directly, but it is there when you
compose your own:

```java
SessionStore<MyState> states = new SessionStore<>();
MyState s = states.getOrCreate(uuid, MyState::new);   // created once per player
// ... on quit:
states.remove(uuid);
```

`Confidence` turns whatever units your check works in into the `[0, 1]` a
`Violation` requires:

```java
Confidence.clamp(v);                 // pin into [0, 1]
Confidence.ramp(v, floor, ceiling);  // 0 at floor, 1 at ceiling, linear between
Confidence.saturating(v, scale);     // v / scale, capped at 1
```

---

## Heuristic checks

A heuristic check follows the classic anticheat shape: each tick either looks
clean or fails with a **weight**; failures raise a per-player **violation
level**, clean ticks **decay** it, and the check flags once the level crosses a
threshold. `AbstractHeuristicCheck` runs that machinery — you implement only the
per-tick judgement.

```java
AbstractHeuristicCheck(String id,
                       double flagThreshold,    // level at which it starts flagging
                       double saturationLevel,  // level at which confidence reaches 1
                       double decayPerPass);    // shed per clean tick
```

### Example: a speed check

```java
import com.github.axiom.ac.api.PlayerData;
import com.github.axiom.ac.detect.heuristic.*;

public final class SpeedCheck extends AbstractHeuristicCheck {

    private static final double MAX_HORIZONTAL = 0.8;

    public SpeedCheck() {
        // flag at level 5, full confidence by 10, shed 1 per clean tick
        super("speed", 5.0, 10.0, 1.0);
    }

    @Override
    protected HeuristicSignal evaluate(PlayerData data) {
        double horizontal = Math.hypot(data.velocity().x(), data.velocity().z());
        return horizontal > MAX_HORIZONTAL
                ? HeuristicSignal.fail(1.0, "horizontal speed " + horizontal)
                : HeuristicSignal.pass();
    }
}
```

With these settings the check tolerates four offending ticks and flags on the
fifth; a clean tick shaves the level back down, so a brief legitimate spike
(knockback, ice) never trips it. `value` on the emitted `Violation` is the
current level, and `confidence` ramps from `flagThreshold` to `saturationLevel`.

### Register it, and forget players on quit

The base holds per-player levels, so release them when a player leaves:

```java
import com.github.axiom.ac.api.event.PlayerQuitEvent;

SpeedCheck speed = new SpeedCheck();
runtime.checks().register(speed);
runtime.eventBus().channel(PlayerQuitEvent.class)
       .subscribe(event -> speed.forget(event.playerId()));
```

### Weighting

`HeuristicSignal.fail(weight, detail)` lets a worse offence count for more — a
small overshoot adds `1.0`, a blatant one adds `5.0`, and the level crosses the
threshold faster:

```java
double over = horizontal - MAX_HORIZONTAL;
return over > 0
        ? HeuristicSignal.fail(1.0 + over * 4.0, "over by " + over)
        : HeuristicSignal.pass();
```

---

## Statistical checks

A statistical check pulls one number per tick into a rolling per-player window,
scores the window with a `StatisticalCriterion`, and flags above a threshold.
`AbstractStatisticalCheck` runs that pipeline; you implement `sample`.

```java
AbstractStatisticalCheck(String id,
                         StatisticalCriterion criterion,
                         int windowSize,         // recent samples scored together
                         double flagScore,       // score at/above which it flags
                         double saturationScore);// score at which confidence reaches 1
```

### The criteria

| Criterion | Score is… | Threshold means | Good for |
|-----------|-----------|-----------------|----------|
| `ZScoreCriterion(minSamples)` | \|z-score\| of the latest sample vs the window | "this many σ from the player's own mean" | values with a stable baseline |
| `IqrCriterion(minSamples)` | overshoot past the Tukey fences, in IQRs (0 inside) | "this far outside the normal spread" | the same, but robust to the outliers it hunts |
| `RegularityCriterion(buckets, minSamples)` | entropy deficit `log2(buckets) − H` | "input collapses into too few buckets" | machine-like regularity |
| `PeriodicityCriterion(minSamples)` | GCD of rounded interval samples | "smallest suspicious period (ms)" | fixed-rate automation |

Empty score = not enough data yet; the check treats that as "no opinion", not a
pass.

### Example: deviation from the player's own speed

```java
import com.github.axiom.ac.api.PlayerData;
import com.github.axiom.ac.detect.statistical.*;
import java.util.OptionalDouble;

public final class SpeedDeviationCheck extends AbstractStatisticalCheck {

    public SpeedDeviationCheck() {
        // 30-sample window; flag at |z| = 3.5, full confidence by 6
        super("speed-deviation", new ZScoreCriterion(10), 30, 3.5, 6.0);
    }

    @Override
    protected OptionalDouble sample(PlayerData data) {
        return OptionalDouble.of(Math.hypot(data.velocity().x(), data.velocity().z()));
    }
}
```

This flags when a player's speed jumps far from their own recent average —
adaptive, so it needs no per-movement-mode tuning. Return `OptionalDouble.empty()`
from `sample` to skip a tick without recording it. Call `forget(uuid)` on quit,
as with heuristic checks.

### Using a criterion directly

`RegularityCriterion` and `PeriodicityCriterion` shine on event timing, which is
not part of `PlayerData` — so feed them your own samples. Track the gaps and
score them with a `SampleAccumulator` (the same per-player window the base uses):

```java
import com.github.axiom.ac.detect.statistical.*;

SampleAccumulator clickGaps = new SampleAccumulator(20);
PeriodicityCriterion periodicity = new PeriodicityCriterion(8);

// On each click, with the millisecond gap since the previous one:
double[] window = clickGaps.record(playerId, gapMillis);
periodicity.score(window).ifPresent(period -> {
    if (period >= 40.0) {
        // every gap is a multiple of `period` ms — a constant-rate autoclicker
    }
});
```

```java
RegularityCriterion regularity = new RegularityCriterion(8, 20);
regularity.score(window).ifPresent(deficit -> {
    if (deficit > 2.0) {
        // gaps cluster into too few buckets — machine-timed
    }
});
```

---

## Raytrace engine

The raytrace subpackage answers geometry questions about the eye ray: *what did
the player point at, how far was it, and could they see it?* It is the basis for
reach, hitbox, and aim checks. All of it is pure math over `axiom-math` and the
cached world from `axiom-world`.

`LookVectors` turns a player's rotation into that ray, matching Minecraft's own
`Location.getDirection()`:

```java
import com.github.axiom.ac.detect.raytrace.*;
import com.github.axiom.ac.math.Vec3;

Vec3 eye  = LookVectors.eyePosition(data.position(), LookVectors.STANDING_EYE_HEIGHT);
Vec3 look = LookVectors.direction(data.yaw(), data.pitch());   // unit vector
```

### Reach

`ReachResolver` measures the distance from the eye to a target box along the
look direction. Expand the box first to absorb the client/server hitbox
mismatch:

```java
import com.github.axiom.ac.math.Aabb;
import java.util.OptionalDouble;

Aabb target = victimBox.expand(0.1, 0.1, 0.1);          // hitbox tolerance
OptionalDouble reach = ReachResolver.distance(eye, look, target);
reach.ifPresent(distance -> {
    if (distance > 3.0) {
        // struck a target beyond survival reach
    }
});
```

Latency makes the exact eye position uncertain, so sweep the candidate eye
positions for the tick and take the *smallest* reach — flag only when even the
most forgiving position is out of range:

```java
import java.util.List;

List<Vec3> eyeCandidates = List.of(previousEye, eye);   // your latency window
OptionalDouble reach = ReachResolver.minimumDistance(eyeCandidates, look, target);
```

### Hitboxes and occlusion

`RaytraceEngine` casts against a set of entity hitboxes and returns the first one
struck. The `nearestVisible` variant drops targets hidden behind solid blocks, so
a hit reflects what the player could actually see. A `Hitbox<T>` carries your own
handle (here the entity UUID), which travels back out on the `RayHit<T>`:

```java
import com.github.axiom.ac.math.Ray;
import java.util.*;

RaytraceEngine raytrace = new RaytraceEngine(runtime.collision());

// Built by you from a snapshot of nearby entities (see the threading note).
List<Hitbox<UUID>> targets = nearbyHitboxes(data);

Optional<RayHit<UUID>> hit = raytrace.nearestVisible(eye, look, 3.5, targets);
hit.ifPresent(h -> {
    UUID victim = h.hitbox().target();
    if (h.distance() > 3.0) {
        // reach: hit a visible entity past the limit
    }
});
```

`LineOfSight` answers the visibility question on its own — useful for an aim
check that fires only when the target was actually exposed:

```java
boolean visible = LineOfSight.clear(eye, victimEyePoint, runtime.collision());
```

> **Threading.** Entity positions come from the Bukkit API, which a check must
> not touch on the packet thread. Build your `Hitbox` list on the main thread and
> hand the packet worker an immutable snapshot. Collision queries are only as
> good as the `WorldCache` — populate it from block updates.

---

## Prediction probe

`axiom-predict` is stateless: give it a previous state and an actual position,
it returns the offset to the closest legitimate single-tick move.
`PredictionProbe` drives it from a live stream — it remembers each player's last
state, computes the offset on every new observation, and keeps a rolling window
so you react to a sustained gap rather than one noisy tick.

Wire a predictor from the runtime's collision engine, then feed the probe inside
a check:

```java
import com.github.axiom.ac.api.*;
import com.github.axiom.ac.detect.prediction.PredictionProbe;
import com.github.axiom.ac.detect.signal.Confidence;
import com.github.axiom.ac.predict.*;
import com.github.axiom.ac.world.PhysicsSimulator;
import java.util.Optional;
import java.util.OptionalDouble;

public final class PredictionCheck implements Check {

    private final PredictionProbe probe;

    public PredictionCheck(PredictionProbe probe) {
        this.probe = probe;
    }

    @Override
    public String id() {
        return "prediction";
    }

    @Override
    public Optional<Violation> inspect(PlayerData data) {
        PlayerState state = new PlayerState(
                data.position(), data.velocity(), data.yaw(), data.onGround());
        probe.observe(data.uuid(), state);

        OptionalDouble average = probe.averageOffset(data.uuid());
        if (average.isPresent() && average.getAsDouble() > 0.1) {
            double offset = average.getAsDouble();
            return Optional.of(new Violation(id(),
                    "avg prediction offset " + offset, offset,
                    Confidence.saturating(offset, 0.5)));
        }
        return Optional.empty();
    }
}
```

```java
// Wiring (once, when registering the check):
PhysicsSimulator simulator = new PhysicsSimulator(runtime.collision());
MovementPredictor predictor = new MovementPredictor(new PredictionEngine(simulator));
PredictionProbe probe = new PredictionProbe(predictor, 20);

PredictionCheck check = new PredictionCheck(probe);
runtime.checks().register(check);
runtime.eventBus().channel(com.github.axiom.ac.api.event.PlayerQuitEvent.class)
       .subscribe(event -> probe.forget(event.playerId()));
```

> **Accuracy.** The `PredictionEngine` movement constants are a documented
> baseline approximation, not yet calibrated to a specific Minecraft version.
> Treat the offset as a *relative* signal — compare a player against their own
> history and against other players — until you tune the constants. See
> [API Reference → axiom-predict](API-Reference.md#axiom-predict).

---

## Lifecycle and threading

- **Forget players on quit.** Every stateful piece — `AbstractHeuristicCheck`,
  `AbstractStatisticalCheck`, `SampleAccumulator`, `PredictionProbe`,
  `SessionStore` — exposes `forget(uuid)` (or `remove`). Wire it to
  `PlayerQuitEvent` so per-player state does not leak.
- **One player, one thread.** Each player's state is touched only on that
  player's packet thread; the stores handle the cross-thread join/quit
  bookkeeping. Do not share a single player's `ViolationLevel` or window across
  threads.
- **No Bukkit API on the packet thread.** Same contract as any `Check`: read the
  `PlayerData`, and bounce to the main thread from a `FlagEvent` subscriber for
  anything Bukkit.

## See also

- [Writing a Check](Writing-a-Check.md) — the underlying `Check` SPI and the flag flow
- [API Reference → axiom-detect](API-Reference.md#axiom-detect) — every type and signature
- [Architecture](Architecture.md) — where `axiom-detect` sits in the module graph
