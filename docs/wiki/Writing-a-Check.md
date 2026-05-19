# Writing a Check

A **check** is your detection logic. It is the one thing Axiom does not provide
— you write it. This page walks the full workflow.

## 1. Implement `Check`

The SPI lives in `axiom-api`:

```java
public interface Check {
    String id();                                // stable, unique
    Optional<Violation> inspect(PlayerData data); // empty = nothing detected
}
```

A check is **stateless** — any per-player memory belongs in `PlayerData` or in
storage. It runs on a packet thread, so it must not call the Bukkit API.

### Example: a simple speed check

```java
import com.github.axiom.ac.api.*;

public final class SpeedCheck implements Check {

    private static final double MAX_HORIZONTAL = 0.8;

    @Override
    public String id() {
        return "speed";
    }

    @Override
    public Optional<Violation> inspect(PlayerData data) {
        double horizontal = Math.hypot(data.velocity().x(), data.velocity().z());
        if (horizontal > MAX_HORIZONTAL) {
            double confidence = Math.min(1.0, horizontal / MAX_HORIZONTAL - 1.0);
            return Optional.of(new Violation(id(),
                    "horizontal speed " + horizontal, horizontal, confidence));
        }
        return Optional.empty();
    }
}
```

`Violation(checkId, description, value, confidence)` — `value` is a magnitude
you define; `confidence` must be in `[0, 1]`.

## 2. Use the math toolkit

Checks become precise by using `axiom-math` instead of ad-hoc arithmetic.
Keep per-player state in a field of a custom `PlayerData` or, for a quick
prototype, a `Map` keyed by UUID.

```java
// Detect a constant click/packet period — a classic automation signal.
long[] intervals = recentPacketIntervals(data.uuid());
long period = Gcd.gcdOf(intervals);
if (period > 40) {
    return Optional.of(new Violation(id(), "constant " + period + "ms period",
            period, 0.9));
}
```

```java
// Flag a statistical outlier against a sliding window of samples.
SlidingWindow window = windowFor(data.uuid());
window.add(sample);
if (window.isFull() && Outliers.zScore(sample, window.mean(),
        window.standardDeviation()) > 3.5) {
    return Optional.of(new Violation(id(), "z-score outlier", sample, 0.8));
}
```

A raw measurement rarely makes a good `confidence` directly. `Normalizer` maps
it onto `[0, 1]`: `minMax(value, floor, ceiling)` for a linear band, or
`softScore(value, midpoint, steepness)` for a graded logistic confidence.

```java
double confidence = Normalizer.softScore(horizontal, MAX_HORIZONTAL, 8.0);
```

See [API Reference → axiom-math](API-Reference.md#axiom-math) for everything
available.

## 3. Use the world and physics (optional)

If your check needs collision or physics, get the engines from the runtime:

```java
CollisionEngine collision = runtime.collision();
Aabb playerBox = new Aabb(/* around data.position() */);
boolean clipping = collision.collides(playerBox);   // inside a solid block?
```

The `WorldCache` must be populated with block data for collision queries to be
meaningful — see [API Reference → axiom-world](API-Reference.md#axiom-world).

## 4. Register the check

```java
AxiomRuntime runtime = AxiomProvider.get().orElseThrow();
runtime.checks().register(new SpeedCheck());
```

The `CheckRegistry` isolates faults: if your check throws, the others keep
running. After repeated consecutive exceptions the check is auto-disabled and a
`CheckFaultEvent` is published — subscribe to it to learn your check is buggy.

## 5. React to flags

Every violation becomes a `FlagEvent` on the bus:

```java
runtime.eventBus().channel(FlagEvent.class).subscribe(event -> {
    Violation v = event.violation();
    getLogger().warning(event.playerId() + " | " + v.checkId() + " | " + v.description());

    // Cancel the event to stop Axiom persisting this violation.
    if (v.confidence() < 0.5) {
        event.setCancelled(true);
    }

    // To act on the Bukkit API, bounce to the main thread:
    // Bukkit.getScheduler().runTask(plugin, () -> kick(event.playerId()));
});
```

A `FlagEvent` that no subscriber cancels is saved through the configured
`StorageProvider`.

## 6. Trigger inspections

Checks run when you call `AxiomRuntime.inspect(playerId)`. Wire it to a
scheduler or your own packet hook — see
[Getting Started → Triggering inspections](Getting-Started.md#triggering-inspections).

## Checklist

- [ ] `id()` is stable and unique.
- [ ] The check is stateless; per-player state lives elsewhere.
- [ ] No Bukkit API calls inside `inspect`.
- [ ] `confidence` is within `[0, 1]`.
- [ ] You subscribe to `CheckFaultEvent` to catch your own bugs.
