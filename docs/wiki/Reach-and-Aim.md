# Reach & Aim

Axiom ships no detection checks — but it ships the geometry and the
rotation tracking those checks need. This page shows how to build a
**reach** check and an **aim** check on top of the toolkit.

## What the toolkit gives you

| Need | Type | Module |
|------|------|--------|
| Look angle, look vector, signed angle deltas | `Rotation` | `axiom-math` |
| Eye position, hitboxes, reach distance, line of sight | `CombatMath` | `axiom-math` |
| Rotation-pattern analysis (deltas, snaps, GCD) | `AimAnalysis` | `axiom-math` |
| Per-player rotation history | `PlayerData.rotationHistory()` | `axiom-api` |

`PlayerData` exposes the live rotation state every check receives:

- `rotation()` — the current look angle.
- `previousRotation()` — the angle before the most recent change.
- `rotationHistory()` — recent angles, oldest first (bounded to 20).

The packet pipeline appends to the history on every rotation packet, so
a check reads it directly with no bookkeeping of its own.

## Reach

Reach is the distance from the attacker's **eye** to the nearest point
of the victim's **hitbox**. `CombatMath` does the geometry.

```java
public final class ReachCheck implements Check {

    /** Survival reach is ~3.0; flag clearly beyond it. */
    private static final double MAX_REACH = 3.0;

    private final Function<UUID, PlayerData> targetLookup;

    public ReachCheck(Function<UUID, PlayerData> targetLookup) {
        this.targetLookup = targetLookup;
    }

    @Override public String id() { return "reach"; }

    @Override public Optional<Violation> inspect(PlayerData attacker) {
        PlayerData victim = currentAttackTarget(attacker); // your combat tracking
        if (victim == null) {
            return Optional.empty();
        }
        Vec3 eye = CombatMath.eyePosition(attacker.position());
        Aabb hitbox = CombatMath.playerHitbox(victim.position());
        double reach = CombatMath.reachDistance(eye, hitbox);
        if (reach > MAX_REACH) {
            return Optional.of(new Violation(id(),
                    "attacked from " + reach + " blocks", reach, 1.0));
        }
        return Optional.empty();
    }
}
```

Latency widens the real hitbox: a moving victim is somewhere between
its last two reported positions by the time the attack arrives. Expand
the hitbox by the victim's per-tick velocity, or compare against the
`TransactionManager.smoothedRoundTrip()` window, before flagging.

## Aim

A reach hit can still be legitimate; an **aim** hit is one the
crosshair never actually crossed. `CombatMath.lineOfSightDistance`
intersects the look ray with the hitbox — empty means the attacker was
not pointing at the victim at all.

```java
Vec3 eye = CombatMath.eyePosition(attacker.position());
Aabb hitbox = CombatMath.playerHitbox(victim.position());
boolean aimed = CombatMath.looksAt(eye, attacker.rotation(), hitbox, MAX_REACH);
if (!aimed) {
    // Hit registered, but the crosshair did not cross the hitbox —
    // the rotation was likely snapped on after the swing.
}
```

## Aim patterns

`AimAnalysis` turns a rotation history into the delta sequences that
expose automated aiming.

```java
List<Rotation> history = attacker.rotationHistory();
if (history.size() < 4) {
    return Optional.empty(); // not enough samples yet
}

double[] yawDeltas = AimAnalysis.yawDeltas(history);
double[] changes   = AimAnalysis.angularChanges(history);

// A large, abrupt step is a snap toward a target.
if (AimAnalysis.hasSnap(history, 60.0)) { /* suspicious */ }

// Real mouse input is quantised by sensitivity: every step is a clean
// multiple of one unit. A divisor that collapses toward the rounding
// unit means fractional, injected corrections were mixed in.
long step = AimAnalysis.quantizationGcd(yawDeltas, 100.0);

// Inhuman consistency: near-zero variance in the angular change.
double smoothness = Stats.standardDeviation(changes);
```

Combine the signals — a snap, a broken sensitivity GCD, and
unnaturally low variance together are far stronger evidence than any
one alone. Set the `Violation` confidence from how many agree.

## See also

- [Writing a Check](Writing-a-Check.md) — the check workflow and the event bus.
- [API Reference](API-Reference.md) — every type, method by method.
