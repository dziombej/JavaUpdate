# Migrating from Java 11 to 25

The Polish companion document [migracja-8-11.md](migracja-8-11.md) covers the
8 → 11 jump. This one picks up where it ends.

The good news first: **11 → 25 is far less painful than 8 → 11.** The module
system landed in 9 and the hard work of that transition is behind you. Most
applications move by changing one number and upgrading a handful of libraries.

The bad news is concentrated in one place: **Java 17 finished what Java 9
started** and closed reflective access to JDK internals for good.

---

## 1. Strong encapsulation of JDK internals — the big one

The timeline is worth having in your head, because the behaviour changed three
times:

| Version | Default | Effect |
|---|---|---|
| 9 – 15 | `--illegal-access=permit` | works, with a warning on first use |
| 16 | `--illegal-access=deny` (JEP 396) | **fails**, unless you pass `permit` |
| 17+ | flag removed entirely (JEP 403) | fails; the flag no longer does anything |

So an application that ran on Java 11 with warnings will **not start** on 17
unless every reflective access is granted explicitly.

The warning you ignored on 11:

```
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by com.example.Foo to field java.lang.String.value
```

becomes on 17:

```
java.lang.reflect.InaccessibleObjectException: Unable to make field private final byte[]
java.lang.String.value accessible: module java.base does not "opens java.lang" to unnamed module
```

### The fix

Per-package flags, at every JVM launch:

```bash
--add-opens   java.base/java.lang=ALL-UNNAMED       # reflection, setAccessible
--add-exports java.base/sun.nio.ch=ALL-UNNAMED      # compile-time and runtime access
```

`--add-opens` grants **deep reflective** access (including private members);
`--add-exports` grants ordinary access to a non-exported package. You almost
always want `--add-opens`.

**Pass them everywhere a JVM starts**, which is more places than people expect:
`java`, Surefire and Failsafe, `MAVEN_OPTS`/`GRADLE_OPTS`, the IDE run
configuration, the container `ENTRYPOINT`. A forgotten Surefire configuration is
the classic — the application starts, the tests do not.

They can also live in `META-INF/MANIFEST.MF` as `Add-Opens:` for an executable
JAR, or in a `JDK_JAVA_OPTIONS` environment variable, which the launcher honours
from Java 9 onwards.

### Finding what you need

```bash
jdeps --jdk-internals --multi-release 25 myapp.jar
```

Reports every use of an internal API along with the supported replacement.

`sun.misc.Unsafe` remains accessible throughout — it lives in `jdk.unsupported`,
which is open by design. Its **memory-access methods** were deprecated for
removal in Java 23, with `VarHandle` (9) and the FFM API (22) as replacements.
Everything else in `Unsafe` is on the same path.

---

## 2. Removed and disabled things

| Removed | Version | What to do |
|---|---|---|
| **Nashorn** JavaScript engine | 15 | GraalJS, or drop the scripting |
| **CMS collector** | 14 | `-XX:+UseConcMarkSweepGC` stops the JVM from starting. Move to G1 (default) or ZGC |
| **RMI Activation** | 17 | |
| **Applet API** | 17 (deprecated), 21 (removed) | |
| **Security Manager** | 17 (deprecated), 24 (permanently disabled) | `System.setSecurityManager` throws. There is no direct replacement — the guidance is OS-level and container-level isolation |
| **Non-generational ZGC** | 24 | `-XX:-ZGenerational` is gone; generational is the only mode |
| **32-bit x86 port** | 24 (removed), Windows 32-bit 21 (deprecated) | |
| **`finalize()`** | 18 deprecated for removal; disabled by default in 25 | `Cleaner`, or try-with-resources |
| **Biased locking** | 15 disabled, 18 removed | Nothing to do; may change `synchronized` performance characteristics under low contention |

---

## 3. UTF-8 by default (Java 18)

Covered in detail in [java18-20.md](java18-20.md). The migration-relevant part:

Every API that takes no explicit `Charset` — `FileReader`, `FileWriter`,
`PrintWriter`, `Scanner`, `String.getBytes()`, `new String(byte[])`,
`Properties.load(InputStream)` and friends — switched from the platform charset
to UTF-8.

If your application **wrote** files with the platform charset on Java 11 and
**reads them back** on 18+, the encodings no longer match. Symptoms are mojibake
in data your own system produced.

Escape hatches, in order of preference:

1. Pass an explicit `Charset` everywhere. The permanent fix.
2. `-Dfile.encoding=COMPAT` — exactly the Java 17 behaviour.
3. `-Dfile.encoding=windows-1250` — pin one legacy charset.

Use `System.getProperty("native.encoding")` when you genuinely need what the OS
uses — talking to a spawned process, a console, or a file another program wrote.

---

## 4. Tool and library floors

Most 11 → 25 failures are really "this library predates the class-file version".

| Tool | Minimum for Java 25 |
|---|---|
| Maven | 3.9+ |
| maven-compiler-plugin | 3.13+ |
| maven-surefire-plugin | 3.2+ |
| Gradle | 8.10+ (check the compatibility matrix per JDK) |
| ASM | 9.7+ |
| Byte Buddy | 1.15+ |
| Mockito | 5.x |
| Lombok | 1.18.34+ |
| Spring Framework | 6.1+ |
| Spring Boot | 3.3+ (Boot 3.x requires Java 17 minimum) |
| Hibernate | 6.4+ |
| Jackson | 2.17+ |
| Jacoco | 0.8.12+ |
| Kotlin | 2.0+ |

The error that means "upgrade the library, not your code":

```
java.lang.IllegalArgumentException: Unsupported class file major version 69
```

**major − 44 = the Java release.** 55 = Java 11, 61 = 17, 65 = 21, 69 = 25. Worth
memorising — it turns an opaque number into a diagnosis. The Class-File API
(Java 24) exists specifically so these errors become rarer as libraries migrate
off ASM.

Use `<release>` rather than `<source>`/`<target>`:

```xml
<maven.compiler.release>25</maven.compiler.release>
```

`source`/`target` only control syntax and bytecode version; `release` compiles
against that version's API signatures, so you cannot accidentally call a method
that does not exist on the target.

---

## 5. `javax.*` → `jakarta.*` — a separate migration

If you are on Spring Boot 2.x and moving to 3.x, you will hit this. **It is not a
Java change.** Jakarta EE 9 renamed every `javax.*` package to `jakarta.*`, and
Spring Boot 3 requires it.

```java
javax.servlet.http.HttpServletRequest   →  jakarta.servlet.http.HttpServletRequest
javax.persistence.Entity                →  jakarta.persistence.Entity
javax.validation.constraints.NotNull    →  jakarta.validation.constraints.NotNull
```

Treat it as its own project. Doing it in the same commit as a JDK upgrade makes
both harder to debug. The Eclipse Transformer and OpenRewrite both automate the
bulk of it.

---

## 6. GC and JVM flags

**G1 is still the default** and has improved steadily. The realistic alternatives
on 21+ are generational ZGC (sub-millisecond pauses, larger footprint) and
generational Shenandoah (25).

Flags to revisit:

```bash
-XX:MaxRAMPercentage=75.0            # instead of a fixed -Xmx in containers
-XX:+UseCompactObjectHeaders         # Java 25; typically 10-20% heap saving
-XX:AOTCacheOutput=app.aot           # Java 25; training run
-XX:AOTCache=app.aot                 # Java 25; subsequent runs start faster
-Xlog:gc*:file=gc.log:time,uptime    # unified logging - the Java 8 flags are long gone
```

`--enable-native-access=ALL-UNNAMED` becomes relevant once anything in your
dependency tree uses the FFM API. Currently a warning; intended to become an
error.

---

## 7. A migration order that works

1. **Stay on `release=11`, but build and test with JDK 25.** Upgrade tools and
   libraries first, without touching code. If the build passes, most of the risk
   is behind you.
2. **Run `jdeps --jdk-internals`** on the built artefacts and fix or flag every
   internal API use. This is the step that prevents the Java 17 surprise.
3. **Raise `release` to 17, then 21, then 25.** One step at a time makes it
   obvious which change caused which failure.
4. **Watch for charset problems** after the 18 boundary — especially in code
   that reads files written by an older version of itself.
5. **Update JVM flags** in start scripts, Dockerfiles and CI.
6. **Only then** start using new language features. The migration and the
   rewrite are separate projects.

---

## 8. What you can start using once you are there

Ordered by how much they change everyday code:

1. **Records** (16) — DTOs, value objects, map keys, multiple return values.
2. **Pattern matching for `instanceof`** (16) — every hand-written `equals`,
   every type-dispatch chain.
3. **Text blocks** (15) — SQL, JSON, HTML.
4. **Switch expressions** (14) and **sealed types** (17) — exhaustive domain
   modelling with compile-time checking.
5. **Virtual threads** (21) — if you have a blocking-I/O service, this is the
   one with the largest operational impact.
6. **Sequenced collections** (21) — small, but it removes a recurring annoyance.
7. **Stream gatherers** (24) — particularly `mapConcurrent` as a replacement for
   `parallel()` on I/O-bound work.
8. **Scoped values** (25) — wherever `ThreadLocal` currently carries request
   context.

The sequence that repays study most is **records → sealed types → pattern
matching**. Individually each is a convenience; together they are a different way
of modelling a domain, with the compiler checking that every case is handled.
