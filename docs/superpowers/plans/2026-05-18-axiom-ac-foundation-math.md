# Axiom AC — Foundation & Math Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Set up the Gradle multi-module skeleton and build `axiom-math`, the pure, dependency-free mathematical toolkit, fully unit-tested.

**Architecture:** A Gradle multi-module project. The root has no source. `axiom-math` is a standalone module with zero external dependencies — only the JDK and JUnit. Every class is pure and deterministic, so the whole module is exercised with plain unit tests.

**Tech Stack:** Java 21, Gradle (multi-module), JUnit 6 (Jupiter).

This plan is **Plan 1 of 4** for the Axiom AC toolkit. It implements the `axiom-math` module from `docs/superpowers/specs/2026-05-18-axiom-ac-design.md`. Plans 2-4 (`axiom-api`/`axiom-packet`, `axiom-world`, `axiom-core`/`axiom-plugin`) follow as separate specs/plans.

---

## File Structure

```
axiom-libraryac/
├── settings.gradle                 MODIFY — declare modules
├── build.gradle                    MODIFY — root: shared subproject config, no source
├── axiom-math/
│   ├── build.gradle                CREATE — empty (config inherited from root)
│   └── src/
│       ├── main/java/com/github/axiom/ac/math/
│       │   ├── Vec3.java           Immutable 3D vector
│       │   ├── Aabb.java           Axis-aligned bounding box
│       │   ├── Ray.java            Ray + ray-AABB intersection
│       │   ├── Stats.java          mean / variance / stddev / min / max
│       │   ├── Distribution.java   skewness / kurtosis / entropy
│       │   ├── Gcd.java            greatest common divisor / periodicity
│       │   ├── Outliers.java       z-score / percentile / IQR bounds
│       │   ├── RollingBuffer.java  Fixed-capacity ring buffer
│       │   ├── SlidingWindow.java  Double-specialised sliding window + stats
│       │   └── MotionFormulas.java Minecraft physics formulas (pure)
│       └── test/java/com/github/axiom/ac/math/
│           └── (one *Test.java per class above)
└── src/                            DELETE — starter Main.java removed
```

Each math class has one responsibility and lives in its own file. Geometry types (`Vec3`, `Aabb`, `Ray`) are immutable records. Statistical helpers (`Stats`, `Distribution`, `Gcd`, `Outliers`, `MotionFormulas`) are final classes with private constructors and static methods. `RollingBuffer`/`SlidingWindow` are the only stateful types.

**Windows note:** all commands use the Windows wrapper `.\gradlew.bat` (the environment shell is PowerShell). On Unix substitute `./gradlew`.

---

## Task 1: Gradle multi-module skeleton

**Files:**
- Modify: `settings.gradle`
- Modify: `build.gradle`
- Create: `axiom-math/build.gradle`
- Delete: `src/main/java/com/github/axiom/ac/Main.java` and the root `src/` directory

- [ ] **Step 1: Replace `settings.gradle`**

```groovy
rootProject.name = 'axiom-libraryac'

include 'axiom-math'
```

- [ ] **Step 2: Replace root `build.gradle`**

The root project carries no source — it only configures shared settings for every module.

```groovy
allprojects {
    group = 'com.github.axiom.ac'
    version = '1.0-SNAPSHOT'
}

subprojects {
    apply plugin: 'java'

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation platform('org.junit:junit-bom:6.0.0')
        testImplementation 'org.junit.jupiter:junit-jupiter'
        testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    }

    test {
        useJUnitPlatform()
    }
}
```

- [ ] **Step 3: Create `axiom-math/build.gradle`**

```groovy
// axiom-math: pure mathematical toolkit. No external dependencies —
// JDK and JUnit only. All build configuration is inherited from the
// root `subprojects` block.
```

- [ ] **Step 4: Remove the starter source**

Delete `src/main/java/com/github/axiom/ac/Main.java` and the now-empty root `src/` tree.

Run: `Remove-Item -Recurse -Force src`
Expected: no output; `src/` no longer exists.

- [ ] **Step 5: Verify the module is recognised**

Run: `.\gradlew.bat projects`
Expected: output lists `+--- Project ':axiom-math'`.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "build: set up Gradle multi-module skeleton with axiom-math

Convert the single-project starter into a multi-module build. The
root project carries no source and configures Java 21 and JUnit for
all modules. Add the empty axiom-math module and drop the starter
Main class.

- Declare axiom-math in settings.gradle
- Move shared config into root allprojects/subprojects blocks
- Remove src/main/java/.../Main.java"
```

---

## Task 2: Vec3 — immutable 3D vector

**Files:**
- Create: `axiom-math/src/main/java/com/github/axiom/ac/math/Vec3.java`
- Test: `axiom-math/src/test/java/com/github/axiom/ac/math/Vec3Test.java`

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class Vec3Test {

    private static final double EPS = 1e-9;

    @Test
    void addSubtractScale() {
        Vec3 a = new Vec3(1, 2, 3);
        Vec3 b = new Vec3(4, 5, 6);
        assertEquals(new Vec3(5, 7, 9), a.add(b));
        assertEquals(new Vec3(-3, -3, -3), a.subtract(b));
        assertEquals(new Vec3(2, 4, 6), a.scale(2));
    }

    @Test
    void dotProduct() {
        assertEquals(32.0, new Vec3(1, 2, 3).dot(new Vec3(4, 5, 6)), EPS);
    }

    @Test
    void lengthAndDistance() {
        assertEquals(5.0, new Vec3(3, 4, 0).length(), EPS);
        assertEquals(25.0, new Vec3(3, 4, 0).lengthSquared(), EPS);
        assertEquals(5.0, new Vec3(0, 0, 0).distance(new Vec3(3, 4, 0)), EPS);
    }

    @Test
    void normalizeProducesUnitVector() {
        assertEquals(1.0, new Vec3(0, 0, 7).normalize().length(), EPS);
    }

    @Test
    void normalizeZeroVectorReturnsZero() {
        assertEquals(new Vec3(0, 0, 0), new Vec3(0, 0, 0).normalize());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.Vec3Test"`
Expected: FAIL — compilation error, `Vec3` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.math;

/**
 * Immutable 3D vector of double components. Building block for geometry
 * and physics math.
 */
public record Vec3(double x, double y, double z) {

    public Vec3 add(Vec3 other) {
        return new Vec3(x + other.x, y + other.y, z + other.z);
    }

    public Vec3 subtract(Vec3 other) {
        return new Vec3(x - other.x, y - other.y, z - other.z);
    }

    public Vec3 scale(double factor) {
        return new Vec3(x * factor, y * factor, z * factor);
    }

    public double dot(Vec3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public double distanceSquared(Vec3 other) {
        return subtract(other).lengthSquared();
    }

    public double distance(Vec3 other) {
        return Math.sqrt(distanceSquared(other));
    }

    /** Returns a unit vector in the same direction, or zero if this is zero. */
    public Vec3 normalize() {
        double len = length();
        return len == 0.0 ? this : scale(1.0 / len);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.Vec3Test"`
Expected: PASS — 5 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-math/src/main/java/com/github/axiom/ac/math/Vec3.java axiom-math/src/test/java/com/github/axiom/ac/math/Vec3Test.java
git commit -m "feat: add Vec3 immutable 3D vector to axiom-math"
```

---

## Task 3: Aabb — axis-aligned bounding box

**Files:**
- Create: `axiom-math/src/main/java/com/github/axiom/ac/math/Aabb.java`
- Test: `axiom-math/src/test/java/com/github/axiom/ac/math/AabbTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AabbTest {

    private final Aabb unit = new Aabb(0, 0, 0, 1, 1, 1);

    @Test
    void intersectsOverlappingBox() {
        assertTrue(unit.intersects(new Aabb(0.5, 0.5, 0.5, 2, 2, 2)));
    }

    @Test
    void doesNotIntersectDisjointBox() {
        assertFalse(unit.intersects(new Aabb(2, 2, 2, 3, 3, 3)));
    }

    @Test
    void touchingFacesDoNotIntersect() {
        assertFalse(unit.intersects(new Aabb(1, 0, 0, 2, 1, 1)));
    }

    @Test
    void containsPointInside() {
        assertTrue(unit.contains(new Vec3(0.5, 0.5, 0.5)));
    }

    @Test
    void doesNotContainPointOutside() {
        assertFalse(unit.contains(new Vec3(1.5, 0.5, 0.5)));
    }

    @Test
    void expandGrowsBoxOutward() {
        assertEquals(new Aabb(-1, -1, -1, 2, 2, 2), unit.expand(1, 1, 1));
    }

    @Test
    void offsetTranslatesBox() {
        assertEquals(new Aabb(1, 1, 1, 2, 2, 2), unit.offset(1, 1, 1));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.AabbTest"`
Expected: FAIL — compilation error, `Aabb` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.math;

/**
 * Immutable axis-aligned bounding box, defined by its minimum and
 * maximum corner. Used for collision and hitbox math.
 */
public record Aabb(double minX, double minY, double minZ,
                    double maxX, double maxY, double maxZ) {

    /** True when this box overlaps {@code other}; shared faces do not count. */
    public boolean intersects(Aabb other) {
        return minX < other.maxX && maxX > other.minX
            && minY < other.maxY && maxY > other.minY
            && minZ < other.maxZ && maxZ > other.minZ;
    }

    /** True when {@code point} lies inside or on this box. */
    public boolean contains(Vec3 point) {
        return point.x() >= minX && point.x() <= maxX
            && point.y() >= minY && point.y() <= maxY
            && point.z() >= minZ && point.z() <= maxZ;
    }

    /** Grows the box outward by the given amount on each axis. */
    public Aabb expand(double dx, double dy, double dz) {
        return new Aabb(minX - dx, minY - dy, minZ - dz,
                        maxX + dx, maxY + dy, maxZ + dz);
    }

    /** Translates the box by the given offset. */
    public Aabb offset(double dx, double dy, double dz) {
        return new Aabb(minX + dx, minY + dy, minZ + dz,
                        maxX + dx, maxY + dy, maxZ + dz);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.AabbTest"`
Expected: PASS — 7 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-math/src/main/java/com/github/axiom/ac/math/Aabb.java axiom-math/src/test/java/com/github/axiom/ac/math/AabbTest.java
git commit -m "feat: add Aabb axis-aligned bounding box to axiom-math"
```

---

## Task 4: Ray — ray and ray-AABB intersection

**Files:**
- Create: `axiom-math/src/main/java/com/github/axiom/ac/math/Ray.java`
- Test: `axiom-math/src/test/java/com/github/axiom/ac/math/RayTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class RayTest {

    private static final double EPS = 1e-9;
    private final Aabb box = new Aabb(2, -0.5, -0.5, 3, 0.5, 0.5);

    @Test
    void hitsBoxAhead() {
        Ray ray = new Ray(new Vec3(0, 0, 0), new Vec3(1, 0, 0));
        OptionalDouble t = ray.intersect(box);
        assertTrue(t.isPresent());
        assertEquals(2.0, t.getAsDouble(), EPS);
    }

    @Test
    void missesBoxToTheSide() {
        Ray ray = new Ray(new Vec3(0, 5, 0), new Vec3(1, 0, 0));
        assertFalse(ray.intersect(box).isPresent());
    }

    @Test
    void missesBoxBehind() {
        Ray ray = new Ray(new Vec3(0, 0, 0), new Vec3(-1, 0, 0));
        assertFalse(ray.intersect(box).isPresent());
    }

    @Test
    void originInsideBoxReturnsZeroDistance() {
        Ray ray = new Ray(new Vec3(2.5, 0, 0), new Vec3(1, 0, 0));
        OptionalDouble t = ray.intersect(box);
        assertTrue(t.isPresent());
        assertEquals(0.0, t.getAsDouble(), EPS);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.RayTest"`
Expected: FAIL — compilation error, `Ray` does not exist.

- [ ] **Step 3: Write minimal implementation**

The slab method clips the ray against each axis pair of planes. `direction` need not be normalised; the returned distance `t` is in units of `direction` length.

```java
package com.github.axiom.ac.math;

import java.util.OptionalDouble;

/**
 * A ray defined by an origin and a direction. Used for reach and
 * line-of-sight math against {@link Aabb} hitboxes.
 */
public record Ray(Vec3 origin, Vec3 direction) {

    /**
     * Intersects this ray with {@code box} using the slab method.
     * Returns the distance {@code t} (in units of {@code direction}
     * length) to the first intersection, or empty when the ray
     * misses or only hits behind the origin. When the origin is
     * inside the box, returns 0.
     */
    public OptionalDouble intersect(Aabb box) {
        double[] o = {origin.x(), origin.y(), origin.z()};
        double[] d = {direction.x(), direction.y(), direction.z()};
        double[] lo = {box.minX(), box.minY(), box.minZ()};
        double[] hi = {box.maxX(), box.maxY(), box.maxZ()};

        double tMin = Double.NEGATIVE_INFINITY;
        double tMax = Double.POSITIVE_INFINITY;

        for (int i = 0; i < 3; i++) {
            if (d[i] == 0.0) {
                if (o[i] < lo[i] || o[i] > hi[i]) {
                    return OptionalDouble.empty();
                }
            } else {
                double t1 = (lo[i] - o[i]) / d[i];
                double t2 = (hi[i] - o[i]) / d[i];
                if (t1 > t2) {
                    double tmp = t1;
                    t1 = t2;
                    t2 = tmp;
                }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                if (tMin > tMax) {
                    return OptionalDouble.empty();
                }
            }
        }
        if (tMax < 0.0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(tMin >= 0.0 ? tMin : 0.0);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.RayTest"`
Expected: PASS — 4 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-math/src/main/java/com/github/axiom/ac/math/Ray.java axiom-math/src/test/java/com/github/axiom/ac/math/RayTest.java
git commit -m "feat: add Ray with ray-AABB intersection to axiom-math"
```

---

## Task 5: Stats — basic statistics

**Files:**
- Create: `axiom-math/src/main/java/com/github/axiom/ac/math/Stats.java`
- Test: `axiom-math/src/test/java/com/github/axiom/ac/math/StatsTest.java`

`variance` uses the **population** definition (divide by `n`).

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class StatsTest {

    private static final double EPS = 1e-9;
    private final double[] sample = {2, 4, 4, 4, 5, 5, 7, 9};

    @Test
    void meanOfSample() {
        assertEquals(5.0, Stats.mean(sample), EPS);
    }

    @Test
    void populationVarianceOfSample() {
        assertEquals(4.0, Stats.variance(sample), EPS);
    }

    @Test
    void standardDeviationOfSample() {
        assertEquals(2.0, Stats.standardDeviation(sample), EPS);
    }

    @Test
    void minAndMax() {
        assertEquals(2.0, Stats.min(sample), EPS);
        assertEquals(9.0, Stats.max(sample), EPS);
    }

    @Test
    void emptyArrayThrows() {
        assertThrows(IllegalArgumentException.class, () -> Stats.mean(new double[0]));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.StatsTest"`
Expected: FAIL — compilation error, `Stats` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.math;

/**
 * Basic descriptive statistics over double samples. {@code variance}
 * and {@code standardDeviation} use the population definition.
 */
public final class Stats {

    private Stats() {
    }

    public static double mean(double[] values) {
        requireNonEmpty(values);
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    public static double variance(double[] values) {
        requireNonEmpty(values);
        double mean = mean(values);
        double sum = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sum += diff * diff;
        }
        return sum / values.length;
    }

    public static double standardDeviation(double[] values) {
        return Math.sqrt(variance(values));
    }

    public static double min(double[] values) {
        requireNonEmpty(values);
        double min = values[0];
        for (double v : values) {
            if (v < min) {
                min = v;
            }
        }
        return min;
    }

    public static double max(double[] values) {
        requireNonEmpty(values);
        double max = values[0];
        for (double v : values) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    private static void requireNonEmpty(double[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("values must not be empty");
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.StatsTest"`
Expected: PASS — 5 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-math/src/main/java/com/github/axiom/ac/math/Stats.java axiom-math/src/test/java/com/github/axiom/ac/math/StatsTest.java
git commit -m "feat: add Stats descriptive statistics to axiom-math"
```

---

## Task 6: Distribution — skewness, kurtosis, entropy

**Files:**
- Create: `axiom-math/src/main/java/com/github/axiom/ac/math/Distribution.java`
- Test: `axiom-math/src/test/java/com/github/axiom/ac/math/DistributionTest.java`

`skewness` and `kurtosis` use population moments; `kurtosis` returns **excess** kurtosis (normal distribution → 0). `entropy` is Shannon entropy in bits over a `long[]` of category counts.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DistributionTest {

    private static final double EPS = 1e-9;

    @Test
    void symmetricSampleHasZeroSkewness() {
        double[] symmetric = {1, 2, 3, 4, 5};
        assertEquals(0.0, Distribution.skewness(symmetric), 1e-9);
    }

    @Test
    void rightTailedSampleHasPositiveSkewness() {
        double[] rightTailed = {1, 1, 1, 1, 10};
        assertTrue(Distribution.skewness(rightTailed) > 0);
    }

    @Test
    void twoPointSampleHasNegativeExcessKurtosis() {
        double[] twoPoint = {0, 0, 0, 1, 1, 1};
        assertEquals(-2.0, Distribution.kurtosis(twoPoint), 1e-9);
    }

    @Test
    void uniformCountsGiveMaxEntropy() {
        // Four equally likely categories -> 2 bits.
        assertEquals(2.0, Distribution.entropy(new long[] {5, 5, 5, 5}), EPS);
    }

    @Test
    void singleCategoryGivesZeroEntropy() {
        assertEquals(0.0, Distribution.entropy(new long[] {7, 0, 0}), EPS);
    }

    @Test
    void entropyOfAllZeroCountsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Distribution.entropy(new long[] {0, 0}));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.DistributionTest"`
Expected: FAIL — compilation error, `Distribution` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.math;

/**
 * Distribution-shape metrics: population skewness, excess kurtosis,
 * and Shannon entropy. Used to detect artificial regularity in
 * player input (for example automated clicking).
 */
public final class Distribution {

    private Distribution() {
    }

    /** Population skewness. Zero for a symmetric sample. */
    public static double skewness(double[] values) {
        double mean = Stats.mean(values);
        double sd = Stats.standardDeviation(values);
        if (sd == 0.0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            double z = (v - mean) / sd;
            sum += z * z * z;
        }
        return sum / values.length;
    }

    /** Population excess kurtosis. Zero for a normal distribution. */
    public static double kurtosis(double[] values) {
        double mean = Stats.mean(values);
        double sd = Stats.standardDeviation(values);
        if (sd == 0.0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            double z = (v - mean) / sd;
            sum += z * z * z * z;
        }
        return sum / values.length - 3.0;
    }

    /**
     * Shannon entropy, in bits, over category counts. Categories with
     * a zero count are ignored. Throws when the total count is zero.
     */
    public static double entropy(long[] counts) {
        long total = 0;
        for (long c : counts) {
            if (c < 0) {
                throw new IllegalArgumentException("counts must not be negative");
            }
            total += c;
        }
        if (total == 0) {
            throw new IllegalArgumentException("counts must not all be zero");
        }
        double entropy = 0.0;
        for (long c : counts) {
            if (c > 0) {
                double p = (double) c / total;
                entropy -= p * (Math.log(p) / Math.log(2.0));
            }
        }
        return entropy;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.DistributionTest"`
Expected: PASS — 6 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-math/src/main/java/com/github/axiom/ac/math/Distribution.java axiom-math/src/test/java/com/github/axiom/ac/math/DistributionTest.java
git commit -m "feat: add Distribution skewness/kurtosis/entropy to axiom-math"
```

---

## Task 7: Gcd — greatest common divisor and periodicity

**Files:**
- Create: `axiom-math/src/main/java/com/github/axiom/ac/math/Gcd.java`
- Test: `axiom-math/src/test/java/com/github/axiom/ac/math/GcdTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GcdTest {

    @Test
    void gcdOfTwoValues() {
        assertEquals(6L, Gcd.gcd(54, 24));
    }

    @Test
    void gcdHandlesNegativeAndZero() {
        assertEquals(6L, Gcd.gcd(-54, 24));
        assertEquals(7L, Gcd.gcd(7, 0));
        assertEquals(0L, Gcd.gcd(0, 0));
    }

    @Test
    void gcdOfArrayFindsCommonPeriod() {
        // Click intervals that are all multiples of 50 ms.
        assertEquals(50L, Gcd.gcdOf(new long[] {100, 150, 200, 50}));
    }

    @Test
    void gcdOfSingleElement() {
        assertEquals(42L, Gcd.gcdOf(new long[] {42}));
    }

    @Test
    void gcdOfEmptyArrayThrows() {
        assertThrows(IllegalArgumentException.class, () -> Gcd.gcdOf(new long[0]));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.GcdTest"`
Expected: FAIL — compilation error, `Gcd` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.math;

/**
 * Greatest common divisor utilities. The array form is used for
 * periodicity analysis — a large common divisor across event
 * intervals signals constant-timed automation.
 */
public final class Gcd {

    private Gcd() {
    }

    /** Euclidean GCD. Operands are treated by absolute value. */
    public static long gcd(long a, long b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            long t = b;
            b = a % b;
            a = t;
        }
        return a;
    }

    /** GCD across every element. Throws when {@code values} is empty. */
    public static long gcdOf(long[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("values must not be empty");
        }
        long result = 0;
        for (long v : values) {
            result = gcd(result, v);
        }
        return result;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.GcdTest"`
Expected: PASS — 5 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-math/src/main/java/com/github/axiom/ac/math/Gcd.java axiom-math/src/test/java/com/github/axiom/ac/math/GcdTest.java
git commit -m "feat: add Gcd periodicity analysis to axiom-math"
```

---

## Task 8: Outliers — z-score, percentile, IQR bounds

**Files:**
- Create: `axiom-math/src/main/java/com/github/axiom/ac/math/Outliers.java`
- Test: `axiom-math/src/test/java/com/github/axiom/ac/math/OutliersTest.java`

`percentile` takes a percentile in `[0, 100]` and uses linear interpolation on a sorted copy. `iqrBounds` returns `[lower, upper]` using the standard 1.5×IQR fences.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OutliersTest {

    private static final double EPS = 1e-9;

    @Test
    void zScoreOfValue() {
        assertEquals(2.0, Outliers.zScore(20, 10, 5), EPS);
    }

    @Test
    void zScoreWithZeroStdDevIsZero() {
        assertEquals(0.0, Outliers.zScore(20, 10, 0), EPS);
    }

    @Test
    void percentileBounds() {
        double[] data = {1, 2, 3, 4, 5};
        assertEquals(1.0, Outliers.percentile(data, 0), EPS);
        assertEquals(3.0, Outliers.percentile(data, 50), EPS);
        assertEquals(5.0, Outliers.percentile(data, 100), EPS);
    }

    @Test
    void iqrBoundsForSample() {
        double[] data = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] bounds = Outliers.iqrBounds(data);
        // Q1 = 2.75, Q3 = 6.25, IQR = 3.5 -> [-2.5, 11.5].
        assertEquals(-2.5, bounds[0], EPS);
        assertEquals(11.5, bounds[1], EPS);
    }

    @Test
    void isOutlierDetectsExtremeValue() {
        double[] data = {10, 11, 12, 13, 14, 15, 16, 17};
        assertTrue(Outliers.isOutlier(100, data));
        assertFalse(Outliers.isOutlier(13, data));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.OutliersTest"`
Expected: FAIL — compilation error, `Outliers` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.math;

import java.util.Arrays;

/**
 * Outlier-detection helpers: z-score, interpolated percentiles, and
 * Tukey IQR fences. Used to build adaptive detection thresholds.
 */
public final class Outliers {

    private Outliers() {
    }

    /** Standard score. Returns 0 when {@code stdDev} is 0. */
    public static double zScore(double value, double mean, double stdDev) {
        if (stdDev == 0.0) {
            return 0.0;
        }
        return (value - mean) / stdDev;
    }

    /**
     * Percentile {@code p} (in [0, 100]) by linear interpolation on a
     * sorted copy of {@code data}.
     */
    public static double percentile(double[] data, double p) {
        if (data.length == 0) {
            throw new IllegalArgumentException("data must not be empty");
        }
        if (p < 0.0 || p > 100.0) {
            throw new IllegalArgumentException("p must be in [0, 100]");
        }
        double[] sorted = data.clone();
        Arrays.sort(sorted);
        double rank = p / 100.0 * (sorted.length - 1);
        int low = (int) Math.floor(rank);
        int high = (int) Math.ceil(rank);
        if (low == high) {
            return sorted[low];
        }
        return sorted[low] + (rank - low) * (sorted[high] - sorted[low]);
    }

    /** Lower and upper Tukey fences: {@code [Q1 - 1.5*IQR, Q3 + 1.5*IQR]}. */
    public static double[] iqrBounds(double[] data) {
        double q1 = percentile(data, 25);
        double q3 = percentile(data, 75);
        double iqr = q3 - q1;
        return new double[] {q1 - 1.5 * iqr, q3 + 1.5 * iqr};
    }

    /** True when {@code value} falls outside the IQR fences of {@code data}. */
    public static boolean isOutlier(double value, double[] data) {
        double[] bounds = iqrBounds(data);
        return value < bounds[0] || value > bounds[1];
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.OutliersTest"`
Expected: PASS — 5 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-math/src/main/java/com/github/axiom/ac/math/Outliers.java axiom-math/src/test/java/com/github/axiom/ac/math/OutliersTest.java
git commit -m "feat: add Outliers z-score/percentile/IQR to axiom-math"
```

---

## Task 9: RollingBuffer — fixed-capacity ring buffer

**Files:**
- Create: `axiom-math/src/main/java/com/github/axiom/ac/math/RollingBuffer.java`
- Test: `axiom-math/src/test/java/com/github/axiom/ac/math/RollingBufferTest.java`

A fixed-capacity FIFO. When full, adding evicts the oldest element. `get(0)` is the oldest retained element.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RollingBufferTest {

    @Test
    void rejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new RollingBuffer<Integer>(0));
    }

    @Test
    void addsUpToCapacity() {
        RollingBuffer<Integer> buffer = new RollingBuffer<>(3);
        buffer.add(1);
        buffer.add(2);
        assertEquals(2, buffer.size());
        assertFalse(buffer.isFull());
        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
    }

    @Test
    void evictsOldestWhenFull() {
        RollingBuffer<Integer> buffer = new RollingBuffer<>(3);
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        buffer.add(4);
        assertTrue(buffer.isFull());
        assertEquals(3, buffer.size());
        assertEquals(List.of(2, 3, 4), buffer.toList());
    }

    @Test
    void getOutOfRangeThrows() {
        RollingBuffer<Integer> buffer = new RollingBuffer<>(3);
        buffer.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(1));
    }

    @Test
    void capacityIsReported() {
        assertEquals(5, new RollingBuffer<Integer>(5).capacity());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.RollingBufferTest"`
Expected: FAIL — compilation error, `RollingBuffer` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.math;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed-capacity FIFO ring buffer. Once full, adding a new element
 * evicts the oldest. Index 0 is always the oldest retained element.
 *
 * <p>Not thread-safe — intended for per-player, thread-confined use.
 *
 * @param <T> element type
 */
public final class RollingBuffer<T> {

    private final Object[] data;
    private int head;
    private int size;

    public RollingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.data = new Object[capacity];
    }

    /** Appends {@code value}, evicting the oldest element when full. */
    public void add(T value) {
        if (size < data.length) {
            data[(head + size) % data.length] = value;
            size++;
        } else {
            data[head] = value;
            head = (head + 1) % data.length;
        }
    }

    /** Returns the element at {@code index}, where 0 is the oldest. */
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index: " + index + ", size: " + size);
        }
        return (T) data[(head + index) % data.length];
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return data.length;
    }

    public boolean isFull() {
        return size == data.length;
    }

    /** Returns retained elements, oldest first. */
    public List<T> toList() {
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(get(i));
        }
        return list;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.RollingBufferTest"`
Expected: PASS — 5 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-math/src/main/java/com/github/axiom/ac/math/RollingBuffer.java axiom-math/src/test/java/com/github/axiom/ac/math/RollingBufferTest.java
git commit -m "feat: add RollingBuffer ring buffer to axiom-math"
```

---

## Task 10: SlidingWindow — double window with live statistics

**Files:**
- Create: `axiom-math/src/main/java/com/github/axiom/ac/math/SlidingWindow.java`
- Test: `axiom-math/src/test/java/com/github/axiom/ac/math/SlidingWindowTest.java`

A double-specialised window built on `RollingBuffer<Double>`. Exposes the retained values and statistics over them via `Stats`.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlidingWindowTest {

    private static final double EPS = 1e-9;

    @Test
    void tracksValuesUpToCapacity() {
        SlidingWindow window = new SlidingWindow(3);
        window.add(1);
        window.add(2);
        assertEquals(2, window.size());
        assertArrayEquals(new double[] {1, 2}, window.toArray(), EPS);
    }

    @Test
    void slidesPastCapacity() {
        SlidingWindow window = new SlidingWindow(3);
        window.add(1);
        window.add(2);
        window.add(3);
        window.add(4);
        assertTrue(window.isFull());
        assertArrayEquals(new double[] {2, 3, 4}, window.toArray(), EPS);
    }

    @Test
    void computesMeanAndStdDevOverWindow() {
        SlidingWindow window = new SlidingWindow(4);
        window.add(2);
        window.add(4);
        window.add(4);
        window.add(6);
        assertEquals(4.0, window.mean(), EPS);
        assertEquals(Math.sqrt(2.0), window.standardDeviation(), EPS);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.SlidingWindowTest"`
Expected: FAIL — compilation error, `SlidingWindow` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.math;

/**
 * Fixed-size sliding window over double samples. New samples evict
 * the oldest once the window is full. Statistics are computed over
 * the currently retained samples via {@link Stats}.
 *
 * <p>Not thread-safe — intended for per-player, thread-confined use.
 */
public final class SlidingWindow {

    private final RollingBuffer<Double> buffer;

    public SlidingWindow(int capacity) {
        this.buffer = new RollingBuffer<>(capacity);
    }

    public void add(double value) {
        buffer.add(value);
    }

    public int size() {
        return buffer.size();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public boolean isFull() {
        return buffer.isFull();
    }

    /** Retained samples, oldest first. */
    public double[] toArray() {
        double[] values = new double[buffer.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = buffer.get(i);
        }
        return values;
    }

    /** Mean of the retained samples. Throws when the window is empty. */
    public double mean() {
        return Stats.mean(toArray());
    }

    /** Population standard deviation of the retained samples. */
    public double standardDeviation() {
        return Stats.standardDeviation(toArray());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.SlidingWindowTest"`
Expected: PASS — 3 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-math/src/main/java/com/github/axiom/ac/math/SlidingWindow.java axiom-math/src/test/java/com/github/axiom/ac/math/SlidingWindowTest.java
git commit -m "feat: add SlidingWindow with live statistics to axiom-math"
```

---

## Task 11: MotionFormulas — Minecraft physics formulas

**Files:**
- Create: `axiom-math/src/main/java/com/github/axiom/ac/math/MotionFormulas.java`
- Test: `axiom-math/src/test/java/com/github/axiom/ac/math/MotionFormulasTest.java`

Pure, collision-free physics formulas for Minecraft player movement. These are the building blocks consumed later by `axiom-world`'s collision-aware `PhysicsSimulator`. Constants target the 1.21 player movement model.

- [ ] **Step 1: Write the failing test**

```java
package com.github.axiom.ac.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MotionFormulasTest {

    private static final double EPS = 1e-9;

    @Test
    void verticalVelocityDecaysWithGravityAndDrag() {
        // From rest: (0 - 0.08) * 0.98 = -0.0784.
        assertEquals(-0.0784, MotionFormulas.nextVerticalVelocity(0.0), EPS);
    }

    @Test
    void horizontalFrictionCombinesSlipperinessAndAirFriction() {
        // Default ground: 0.6 * 0.91 = 0.546.
        assertEquals(0.546, MotionFormulas.horizontalFriction(0.6), EPS);
    }

    @Test
    void horizontalVelocityScalesByFriction() {
        assertEquals(0.546, MotionFormulas.nextHorizontalVelocity(1.0, 0.546), EPS);
    }

    @Test
    void jumpVelocityWithoutBoost() {
        assertEquals(0.42, MotionFormulas.jumpVelocity(0), EPS);
    }

    @Test
    void jumpVelocityWithBoost() {
        // Jump Boost II adds 2 * 0.1.
        assertEquals(0.62, MotionFormulas.jumpVelocity(2), EPS);
    }

    @Test
    void negativeJumpBoostThrows() {
        assertThrows(IllegalArgumentException.class, () -> MotionFormulas.jumpVelocity(-1));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.MotionFormulasTest"`
Expected: FAIL — compilation error, `MotionFormulas` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.github.axiom.ac.math;

/**
 * Pure Minecraft player-movement formulas, with no world collision.
 * These are the building blocks of collision-aware physics
 * simulation, kept here so they are testable in isolation.
 *
 * <p>Constants target the 1.21 player movement model.
 */
public final class MotionFormulas {

    /** Downward acceleration applied per tick. */
    public static final double GRAVITY = 0.08;

    /** Per-tick multiplier applied to vertical velocity. */
    public static final double VERTICAL_DRAG = 0.98;

    /** Base horizontal air friction multiplier. */
    public static final double AIR_FRICTION = 0.91;

    /** Block slipperiness of an ordinary (non-ice) block. */
    public static final double DEFAULT_SLIPPERINESS = 0.6;

    /** Vertical velocity of a jump with no Jump Boost effect. */
    public static final double BASE_JUMP_VELOCITY = 0.42;

    /** Extra jump velocity per Jump Boost amplifier level. */
    public static final double JUMP_BOOST_PER_LEVEL = 0.1;

    private MotionFormulas() {
    }

    /** Vertical velocity after one tick of gravity and drag. */
    public static double nextVerticalVelocity(double velocityY) {
        return (velocityY - GRAVITY) * VERTICAL_DRAG;
    }

    /**
     * Effective horizontal friction multiplier for a block of the
     * given {@code slipperiness}.
     */
    public static double horizontalFriction(double slipperiness) {
        return slipperiness * AIR_FRICTION;
    }

    /** Horizontal velocity after one tick of the given friction. */
    public static double nextHorizontalVelocity(double velocity, double friction) {
        return velocity * friction;
    }

    /**
     * Initial vertical velocity of a jump with the given Jump Boost
     * amplifier level (0 = no effect).
     */
    public static double jumpVelocity(int jumpBoostLevel) {
        if (jumpBoostLevel < 0) {
            throw new IllegalArgumentException("jumpBoostLevel must be >= 0");
        }
        return BASE_JUMP_VELOCITY + JUMP_BOOST_PER_LEVEL * jumpBoostLevel;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :axiom-math:test --tests "com.github.axiom.ac.math.MotionFormulasTest"`
Expected: PASS — 6 tests.

- [ ] **Step 5: Commit**

```bash
git add axiom-math/src/main/java/com/github/axiom/ac/math/MotionFormulas.java axiom-math/src/test/java/com/github/axiom/ac/math/MotionFormulasTest.java
git commit -m "feat: add MotionFormulas Minecraft physics to axiom-math"
```

---

## Task 12: Full module verification

**Files:** none — verification only.

- [ ] **Step 1: Run the whole module test suite**

Run: `.\gradlew.bat :axiom-math:test`
Expected: PASS — `BUILD SUCCESSFUL`, all 10 test classes green (51 tests total).

- [ ] **Step 2: Build the module**

Run: `.\gradlew.bat :axiom-math:build`
Expected: `BUILD SUCCESSFUL`. `axiom-math/build/libs/axiom-math-1.0-SNAPSHOT.jar` is produced.

- [ ] **Step 3: Commit if anything changed**

If steps 1-2 produced no source changes, skip. Otherwise:

```bash
git add -A
git commit -m "build: verify axiom-math module builds and tests pass"
```

---

## Plan Complete

`axiom-math` is now a complete, standalone, fully-tested module: geometry (`Vec3`, `Aabb`, `Ray`), statistics (`Stats`, `Distribution`, `Outliers`, `Gcd`), buffers (`RollingBuffer`, `SlidingWindow`), and physics formulas (`MotionFormulas`).

**Next:** Plan 2 — `axiom-api` contracts + `axiom-packet` (PacketEvents pipeline, `PlayerData`, `TransactionManager`). Requires its own spec section review and plan.
