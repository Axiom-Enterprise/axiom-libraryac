# Getting Started

## Prerequisites

- **Java 21** — the whole project targets the Java 21 toolchain.
- **Paper 1.21.x** — required at runtime for `axiom-plugin`.
- **PacketEvents 2.12.1** — installed on the server as a plugin, or shaded into
  your own plugin.
- **Gradle** — the wrapper (`gradlew` / `gradlew.bat`) is included; no global
  install needed.

## Building from source

```bash
git clone <repository-url>
cd axiom-libraryac
./gradlew build          # Windows: .\gradlew.bat build
```

This compiles and tests all eight modules. Each module's jar is written to
`<module>/build/libs/`.

## Consuming Axiom

Axiom is **not yet published to a Maven repository**. Three practical options:

### 1. Gradle composite build (recommended for development)

In your plugin's `settings.gradle`:

```groovy
includeBuild '../axiom-libraryac'
```

Then depend on the modules you need in `build.gradle`:

```groovy
dependencies {
    implementation 'com.github.axiom.ac:axiom-api'
    implementation 'com.github.axiom.ac:axiom-math'
    implementation 'com.github.axiom.ac:axiom-core'
}
```

### 2. Local jars

Build Axiom, then drop the jars from `<module>/build/libs/` onto your
classpath (`flatDir` or a local file dependency).

### 3. Run the bundled plugin

Build `axiom-plugin`, place
`axiom-plugin/build/libs/axiom-plugin-1.0-SNAPSHOT.jar` and the PacketEvents
plugin in your server's `plugins/` folder. Axiom starts as a shared runtime;
your own plugin reaches it through `AxiomProvider.get()`.

## Which modules do I need?

| Goal | Modules |
|------|---------|
| Just the math (statistics, geometry, physics formulas) | `axiom-math` |
| Write checks against a running Axiom instance | `axiom-api` |
| Embed the full runtime in your own plugin | `axiom-core` (pulls in the rest) |
| Use collision / physics directly | `axiom-world` |
| Use the movement-prediction engine | `axiom-predict` |
| Build heuristic, statistical or raytrace checks | `axiom-detect` |

## Running it

The simplest path is the bundled plugin. Once `axiom-plugin` and PacketEvents
are installed:

```java
// In your own plugin's onEnable, after Axiom has loaded:
AxiomRuntime runtime = AxiomProvider.get().orElseThrow(
        () -> new IllegalStateException("Axiom is not loaded"));

runtime.checks().register(new MyCheck());
runtime.eventBus().channel(FlagEvent.class)
       .subscribe(event -> handleFlag(event));
```

To embed Axiom inside your own plugin instead of using the bundled plugin,
construct the runtime yourself:

```java
AxiomRuntime runtime = new AxiomRuntime(new MemoryStorageProvider());
AxiomProvider.set(runtime);
// register the PacketEvents listener: new PacketPipeline(runtime.players())
```

## Triggering inspections

Axiom decodes packets into `PlayerData` continuously, but **checks are run when
you call `AxiomRuntime.inspect(playerId)`**. Wire this to whatever cadence you
want — a per-tick scheduler, or your own packet hook. Each call runs every
registered check for that player and publishes a `FlagEvent` per violation.

```java
// Example: inspect every tracked player each server tick.
Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    for (var data : runtime.players().all()) {
        runtime.inspect(data.uuid());
    }
}, 1L, 1L);
```

Next: [Writing a Check](Writing-a-Check.md).
