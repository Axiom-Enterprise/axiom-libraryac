# Architecture

## Modules and dependencies

```
axiom-math      (no dependencies — pure JDK)
   ▲
axiom-api ──────┤            axiom-world ──┐
   ▲            │               ▲         │
axiom-packet ───┘               │         │
   ▲                            │         │
axiom-core ─────────────────────┘         │
   ▲                                      │
axiom-plugin                  axiom-predict┘
```

| Module | Depends on | External deps |
|--------|------------|---------------|
| `axiom-math` | — | none |
| `axiom-api` | `axiom-math` | none |
| `axiom-packet` | `axiom-api`, `axiom-math` | PacketEvents (`compileOnly`) |
| `axiom-world` | `axiom-math` | none |
| `axiom-core` | `axiom-api`, `axiom-math`, `axiom-packet`, `axiom-world` | Gson |
| `axiom-plugin` | `axiom-core`, `axiom-packet`, `axiom-api`, `axiom-world` | Paper API, PacketEvents (`compileOnly`) |
| `axiom-predict` | `axiom-math`, `axiom-world` | none |

`axiom-math`, `axiom-world` and `axiom-predict` are pure, dependency-free and
fully deterministic — usable on their own, outside any Minecraft context.

## Data flow

```
client packet
   │  (netty thread)
   ▼
PacketEvents ──▶ PacketPipeline ──▶ PlayerDataImpl.applyMovement(MovementUpdate)
                                         │
                          AxiomRuntime.inspect(uuid)
                                         │
                          CheckRegistry runs every Check
                                         │
                          Violation ──▶ FlagEvent on the EventBus
                                         │
                  not cancelled ──▶ StorageProvider.saveViolation
```

1. PacketEvents delivers a client packet on a netty event-loop thread.
2. `PacketPipeline` decodes movement packets into a `MovementUpdate` and applies
   it to that player's `PlayerDataImpl`.
3. `TransactionManager` issues ping/pong transactions; their round trip bounds
   when the client had received prior server state (latency compensation).
4. When you call `AxiomRuntime.inspect(uuid)`, `CheckRegistry` runs every
   registered `Check` against the player's data.
5. Each `Violation` becomes a `FlagEvent`, published on the `EventBus`.
6. A `FlagEvent` that no subscriber cancelled is persisted via the
   `StorageProvider`.

## Threading model

- **Per-player packet processing runs on that player's netty thread.**
  `PlayerDataImpl` and `TransactionManager` are thread-confined — they carry no
  synchronisation and must only be touched on that thread.
- **Cross-thread structures use concurrent collections.** `PlayerRegistry`,
  `WorldCache`, `CheckRegistry`, and the storage providers use
  `ConcurrentHashMap` / `CopyOnWriteArrayList`. `AxiomProvider` holds the
  runtime in a `volatile` field.
- **Event bus dispatch is synchronous** on the publishing thread, in
  subscription order.

> **Contract:** a `Check` must not call the Bukkit API directly — it runs on a
> packet thread. Inspect the `PlayerData` you are given; if you need the Bukkit
> API, bounce to the main thread from your `FlagEvent` subscriber.

## Runtime model

Axiom runs as a **hybrid**: it can be embedded inside your own plugin
(construct an `AxiomRuntime` yourself) or run as the standalone `axiom-plugin`,
which publishes a single shared runtime through `AxiomProvider`.

## Design decisions

The full rationale — why a toolkit and not a product, why the event bus is
concrete in `axiom-api`, why `axiom-world` is collision-only, the packet-depth
trade-off — is recorded in
[`docs/superpowers/specs/2026-05-18-axiom-ac-design.md`](../superpowers/specs/2026-05-18-axiom-ac-design.md).
