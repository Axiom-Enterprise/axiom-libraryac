# Axiom AC — Wiki

Axiom AC is a **Minecraft (Paper) anticheat toolkit**. It ships no detection
checks; it gives you the framework — math, packets, world, physics, prediction,
and an event bus — to build your own.

## Who this wiki is for

- **Developers** writing an anticheat or a check on top of Axiom.
- **AI agents** working with the codebase — see [For AI Agents](For-AI-Agents.md)
  for a dense, machine-friendly map.

## Pages

| Page | Read it when you want to… |
|------|---------------------------|
| [Getting Started](Getting-Started.md) | Add Axiom to a project and get it running |
| [Architecture](Architecture.md) | Understand the modules, dependencies, and threading model |
| [Writing a Check](Writing-a-Check.md) | Implement detection logic and react to flags |
| [Reach & Aim](Reach-and-Aim.md) | Build reach and aim checks with the combat & rotation utilities |
| [API Reference](API-Reference.md) | Look up a specific type and its methods |
| [For AI Agents](For-AI-Agents.md) | Get a compact, structured overview for tooling |

## The mental model

1. **PacketEvents** feeds raw client packets into Axiom.
2. `axiom-packet` decodes them into per-player `PlayerData` (position, velocity,
   rotation, history).
3. Your `Check` implementations inspect a `PlayerData` snapshot and may return a
   `Violation`.
4. `axiom-core` runs the checks, publishes a `FlagEvent` per violation on the
   event bus, and persists violations through a `StorageProvider`.
5. You subscribe to `FlagEvent` and decide the consequence (alert, kick, ban).

Everything else — the math toolkit, the collision engine, the physics
simulator, the prediction engine — exists so your checks can be precise.

## Core principle

Axiom is a **toolkit, not a product**. It does not decide what is cheating.
It hands you accurate, server-side, latency-compensated data and the math to
reason about it. The detection logic, the thresholds, and the punishment policy
are yours.
