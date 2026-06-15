# Axiom AC

**A Minecraft (Paper) anticheat toolkit library.**

Axiom AC is *not* a finished anticheat — it ships **no detection checks**. It is
the framework you build an anticheat *on*: mathematical primitives, a
PacketEvents-backed packet pipeline with latency compensation, a server-side
world model with collision and physics, a deterministic movement-prediction
engine, check-building scaffolding (heuristic, statistical, raytrace), and a
GrimAPI-style event bus.

You write the `Check`s. Axiom gives you the tools and the runtime.

```
┌─────────────┐   you implement    ┌──────────────────────────────┐
│  your Check │ ─────────────────▶ │  Axiom toolkit + runtime     │
└─────────────┘                    │  math · packets · world ·    │
       ▲                           │  physics · prediction · bus  │
       │  FlagEvent                └──────────────────────────────┘
       └──────────────── you subscribe
```

## Modules

| Module          | What it gives you                                                            |
|-----------------|------------------------------------------------------------------------------|
| `axiom-math`    | Geometry (`Vec3`, `Aabb`, `Ray`), statistics, GCD, outliers, physics formulas |
| `axiom-api`     | Public contracts: `Check`, `PlayerData`, `Violation`, the event bus, SPIs     |
| `axiom-packet`  | PacketEvents pipeline, per-player data, transaction (latency) manager         |
| `axiom-world`   | Block cache, `CollisionEngine` (AABB + voxel raycast), `PhysicsSimulator`     |
| `axiom-core`    | Runtime wiring, check registry with fault isolation, storage providers        |
| `axiom-plugin`  | The thin Paper plugin that bootstraps everything                              |
| `axiom-predict` | Deterministic movement-prediction engine (the offset cheat signal)            |
| `axiom-detect`  | Check-building toolkit: heuristic & statistical check bases, raytrace engine, prediction probe |

## Quick taste

```java
// A check: flag horizontal speed above a fixed threshold.
public final class SpeedCheck implements Check {
    @Override public String id() { return "speed"; }

    @Override public Optional<Violation> inspect(PlayerData data) {
        double horizontal = Math.hypot(data.velocity().x(), data.velocity().z());
        if (horizontal > 1.0) {
            return Optional.of(new Violation(id(), "moved too fast", horizontal, 1.0));
        }
        return Optional.empty();
    }
}

// Register it and react to flags.
AxiomRuntime runtime = AxiomProvider.get().orElseThrow();
runtime.checks().register(new SpeedCheck());
runtime.eventBus().channel(FlagEvent.class).subscribe(event ->
    getLogger().warning(event.playerId() + " flagged: " + event.violation().description()));
```

## Requirements

- Java 21
- Paper 1.21.x (runtime, for `axiom-plugin`)
- PacketEvents 2.12.1 (server-side plugin or shaded)
- Gradle (the wrapper is included)

## Build

```bash
./gradlew build        # build and test every module
./gradlew test         # run the full test suite (243 tests)
```

On Windows use `.\gradlew.bat`.

The library is **not yet published to a Maven repository** — build it from
source and consume the module jars (`<module>/build/libs/`), or wire Axiom in
as a Gradle composite build. See the wiki for details.

## Documentation

Full usage docs live in [`docs/wiki/`](docs/wiki/Home.md):

- [Home](docs/wiki/Home.md) — index and overview
- [Getting Started](docs/wiki/Getting-Started.md) — depend on Axiom, run it
- [Architecture](docs/wiki/Architecture.md) — modules, dependencies, threading
- [Writing a Check](docs/wiki/Writing-a-Check.md) — the core workflow
- [API Reference](docs/wiki/API-Reference.md) — the toolkit type by type
- [For AI Agents](docs/wiki/For-AI-Agents.md) — a dense map for automated tools

The design rationale is in
[`docs/superpowers/specs/`](docs/superpowers/specs) and the implementation
plans in [`docs/superpowers/plans/`](docs/superpowers/plans).

## Status

All seven modules of the original design, plus the `axiom-detect`
check-building toolkit, are implemented and tested (243 unit tests).
The PacketEvents/Paper glue compiles against the real dependencies but should
be exercised on a live server before production use. The `axiom-predict`
physics constants are a documented baseline approximation — see
[the prediction docs](docs/wiki/API-Reference.md#axiom-predict).

## License

See the repository for license terms.
