# Movement Prediction

`axiom-predict` reproduces Minecraft's player-movement model tick by
tick. Given where a player was and how the world looks, it computes
where every legitimate input could have taken them — and a **movement
check** flags the gap between that and where the client claims to be.

## What the toolkit gives you

| Need | Type | Module |
|------|------|--------|
| One tick of physics for a candidate input | `PredictionEngine` | `axiom-predict` |
| The best-matching input and its offset | `MovementPredictor` | `axiom-predict` |
| The candidate input set | `InputSpace` | `axiom-predict` |
| A movement snapshot | `PlayerState` | `axiom-predict` |
| Effects, enchantments, flight state | `MovementContext` | `axiom-predict` |
| Offset → bounded cheat score | `OffsetNormalizer` | `axiom-predict` |
| Collision and walking physics | `PhysicsSimulator` | `axiom-world` |

`axiom-predict` depends only on `axiom-math` and `axiom-world`; it is
pure and deterministic, and runs outside any Minecraft context.

## The model

`PredictionEngine.predict(previous, input, context)` advances one
`PlayerState` by a single tick. Each tick it picks a movement branch —
walking/air, water, lava, powder snow, climbing, or elytra — then
applies that medium's physics: friction or medium decay, input
acceleration, and axis-by-axis collision with step assistance.

Across the branches it also models sprint and sneak rules, Jump Boost,
the sprint-jump impulse, Speed/Slowness, Levitation, Slow Falling,
soul-sand and honey slowdown, slime bounce, cobweb entanglement, the
elytra firework boost, the Riptide launch impulse, water Depth Strider
and Dolphin's Grace, bubble-column currents, and scaffolding
sneak-descent.

> **Accuracy.** Branch selection, tick ordering, and the modifiers are
> exact. The movement *constants* are a documented 1.21+ baseline
> approximation — treat the offset as a relative signal.

## Predicting a move

`MovementPredictor` searches the whole `InputSpace` (72 candidate
inputs) and returns the one whose predicted position lands closest to
the player's actual position.

```java
PhysicsSimulator simulator = new PhysicsSimulator(runtime.collision());
MovementPredictor predictor = new MovementPredictor(new PredictionEngine(simulator));

PlayerState previous = new PlayerState(
        lastPosition, lastVelocity, yaw, pitch, lastOnGround);

PredictionResult result = predictor.bestPrediction(previous, actualPosition);
double offset = result.offset(); // distance, in blocks, no input explains
```

A legitimate move is explained by *some* input, so the offset stays
near zero. A move no input reproduces leaves a large offset — the
cheat signal.

> **Stateful helper.** `bestPrediction` is stateless — you hold the previous
> state yourself. To drive it from a live stream and react to a *sustained* gap
> rather than one noisy tick, `axiom-detect.prediction.PredictionProbe` remembers
> each player's last state and keeps a rolling window of offsets. See
> [Axiom Detect](Axiom-Detect.md#prediction-probe).

## Scoring the offset

A raw offset is never exactly zero: floating-point drift and the
constant approximations leave a small residual. `OffsetNormalizer`
absorbs that with a noise floor and maps the rest onto `[0, 1]`.

```java
double score = OffsetNormalizer.DEFAULT.score(result);   // 0.0 .. 1.0
if (OffsetNormalizer.DEFAULT.isSuspicious(result)) {
    return Optional.of(new Violation("motion",
            "unexplained move of " + result.offset() + " blocks",
            result.offset(), score));
}
```

## The context

`MovementContext` carries the slowly changing state a prediction
depends on but a single input does not — potion effects, worn
enchantments, and the elytra / firework / Riptide flight state. Build
it from server-side knowledge with the fluent builder:

```java
MovementContext context = MovementContext.builder()
        .elytra(true)
        .fireworkBoost(firstTickOfRocket)
        .depthStrider(bootsDepthStriderLevel)
        .build();

PredictionResult result =
        predictor.bestPrediction(previous, actualPosition, context);
```

`InputSpace` enumerates only ground and air inputs; elytra, firework,
and Riptide are not searched — they are deterministic given the
context, which the anticheat already knows from the server.

## See also

- [Writing a Check](Writing-a-Check.md) — the check workflow and the event bus.
- [Axiom Detect](Axiom-Detect.md) — the `PredictionProbe` that drives this engine statefully.
- [API Reference → axiom-predict](API-Reference.md#axiom-predict) — every type.
- [Architecture](Architecture.md) — how the modules fit together.
